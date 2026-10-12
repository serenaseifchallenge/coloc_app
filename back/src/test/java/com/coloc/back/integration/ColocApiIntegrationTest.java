package com.coloc.back.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests d'intégration : de vraies requêtes HTTP traversent toutes les couches
 * (filtre JWT, contrôleur, service, repository) jusqu'à la base PostgreSQL.
 *
 * Prérequis : la base Docker doit tourner ({@code docker compose up -d}).
 * Chaque test est annulé à la fin (@Transactional) : aucune donnée ne reste en base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ColocApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** Un utilisateur de test : son jeton JWT et son identifiant. */
    private record User(String token, long id) {
    }

    // ---------------------------------------------------------------- sécurité

    @Test
    void protectedRouteWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- dépenses

    @Test
    void expenseIsSplitAndBalancesAreComputed() throws Exception {
        User alice = register("Alice");
        String code = createHouse(alice);
        User bob = register("Bob");
        join(bob, code);

        // Alice paie 30 € pour elle et Bob : création (201) puis lecture de la dépense
        String expense = """
                {"name":"Courses","amount":30.00,"expenseDate":"%s","payerId":%d,
                 "category":"COURANTE","participantIds":[%d,%d]}
                """.formatted(LocalDate.now(), alice.id(), alice.id(), bob.id());
        mockMvc.perform(authed(post("/api/expenses"), alice).content(expense))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Courses"))
                .andExpect(jsonPath("$.myShare").value(15.0));

        mockMvc.perform(authed(get("/api/expenses"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Soldes : Alice est créditrice de 15 €, Bob débiteur de 15 €, un seul remboursement conseillé
        String balances = mockMvc.perform(authed(get("/api/balances"), alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedTransfers", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        assertThat(balanceOf(balances, alice)).isEqualTo(15.0);
        assertThat(balanceOf(balances, bob)).isEqualTo(-15.0);
    }

    @Test
    void invalidExpenseIsRejectedWithBadRequest() throws Exception {
        User alice = register("Alice");
        createHouse(alice);

        String negativeAmount = """
                {"name":"Erreur","amount":-5,"expenseDate":"%s","payerId":%d,
                 "category":"AUTRE","participantIds":[%d]}
                """.formatted(LocalDate.now(), alice.id(), alice.id());
        mockMvc.perform(authed(post("/api/expenses"), alice).content(negativeAmount))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expenseOfAnotherHouseIsNotVisible() throws Exception {
        User alice = register("Alice");
        createHouse(alice);
        String expense = """
                {"name":"Privée","amount":10.00,"expenseDate":"%s","payerId":%d,
                 "category":"AUTRE","participantIds":[%d]}
                """.formatted(LocalDate.now(), alice.id(), alice.id());
        String created = mockMvc.perform(authed(post("/api/expenses"), alice).content(expense))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int expenseId = JsonPath.read(created, "$.id");

        // Une personne d'une autre colocation obtient 404, comme si la dépense n'existait pas
        User stranger = register("Camille");
        createHouse(stranger);
        mockMvc.perform(authed(get("/api/expenses/" + expenseId), stranger))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- cagnottes

    @Test
    void onlyThePayerCanWithdrawHerPotPayment() throws Exception {
        User alice = register("Alice");
        String code = createHouse(alice);
        User bob = register("Bob");
        join(bob, code);
        int potId = createPot(alice, 100.00, bob);

        // Bob verse 20 €
        String afterPayment = mockMvc.perform(authed(post("/api/pots/" + potId + "/payments"), bob)
                        .content(payment(20.00)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collected").value(20.0))
                .andReturn().getResponse().getContentAsString();
        int paymentId = JsonPath.read(afterPayment, "$.payments[0].id");

        // Alice ne peut pas retirer le versement de Bob
        mockMvc.perform(authed(delete("/api/pots/" + potId + "/payments/" + paymentId), alice))
                .andExpect(status().isForbidden());

        // Bob retire le sien : la cagnotte repasse à 0 €
        mockMvc.perform(authed(delete("/api/pots/" + potId + "/payments/" + paymentId), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(0.0));
    }

    @Test
    void purchaseCanOnlyBeMarkedOnceThePotIsCompleteAndThenLocksWithdrawals() throws Exception {
        User alice = register("Alice");
        String code = createHouse(alice);
        User bob = register("Bob");
        join(bob, code);
        int potId = createPot(alice, 40.00, bob);

        // Cagnotte incomplète : impossible de la marquer comme achetée
        mockMvc.perform(authed(put("/api/pots/" + potId + "/purchased"), bob).content("{\"purchased\":true}"))
                .andExpect(status().isBadRequest());

        // Alice verse les 40 € : la cagnotte est complète
        String complete = mockMvc.perform(authed(post("/api/pots/" + potId + "/payments"), alice)
                        .content(payment(40.00)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.complete").value(true))
                .andReturn().getResponse().getContentAsString();
        int paymentId = JsonPath.read(complete, "$.payments[0].id");

        // Bob (participant) indique que l'achat est fait
        mockMvc.perform(authed(put("/api/pots/" + potId + "/purchased"), bob).content("{\"purchased\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").value(true));

        // Cagnotte close : Alice ne peut plus retirer son versement
        mockMvc.perform(authed(delete("/api/pots/" + potId + "/payments/" + paymentId), alice))
                .andExpect(status().isBadRequest());
    }

    @Test
    void secretPotIsInvisibleToTheRoommateItIsHiddenFrom() throws Exception {
        User alice = register("Alice");
        String code = createHouse(alice);
        User bob = register("Bob");
        join(bob, code);
        User carol = register("Carol");
        join(carol, code);

        // Alice et Bob organisent un cadeau pour Carol : la cagnotte lui est cachée
        String body = """
                {"name":"Cadeau surprise","type":"CADEAU","targetAmount":60.00,"deadline":null,
                 "participantIds":[%d,%d],"hiddenFromIds":[%d]}
                """.formatted(alice.id(), bob.id(), carol.id());
        String created = mockMvc.perform(authed(post("/api/pots"), alice).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hiddenFromIds[0]").value((int) carol.id()))
                .andReturn().getResponse().getContentAsString();
        int potId = JsonPath.read(created, "$.id");

        // Bob la voit et peut cotiser
        mockMvc.perform(authed(get("/api/pots"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(authed(post("/api/pots/" + potId + "/payments"), bob).content(payment(10.00)))
                .andExpect(status().isCreated());

        // Carol ne la voit nulle part : liste vide, détail, versement et modification donnent 404
        mockMvc.perform(authed(get("/api/pots"), carol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(authed(get("/api/pots/" + potId), carol)).andExpect(status().isNotFound());
        mockMvc.perform(authed(post("/api/pots/" + potId + "/payments"), carol).content(payment(5.00)))
                .andExpect(status().isNotFound());
        mockMvc.perform(authed(delete("/api/pots/" + potId), carol)).andExpect(status().isNotFound());
    }

    @Test
    void invalidHiddenListIsRejected() throws Exception {
        User alice = register("Alice");
        String code = createHouse(alice);
        User bob = register("Bob");
        join(bob, code);

        // Le créateur ne peut pas se cacher sa propre cagnotte
        String hideCreator = """
                {"name":"Test","type":"AUTRE","targetAmount":20.00,"deadline":null,
                 "participantIds":[%d],"hiddenFromIds":[%d]}
                """.formatted(bob.id(), alice.id());
        mockMvc.perform(authed(post("/api/pots"), alice).content(hideCreator))
                .andExpect(status().isBadRequest());

        // Une personne à qui la cagnotte est cachée ne peut pas y participer
        String hideParticipant = """
                {"name":"Test","type":"AUTRE","targetAmount":20.00,"deadline":null,
                 "participantIds":[%d,%d],"hiddenFromIds":[%d]}
                """.formatted(alice.id(), bob.id(), bob.id());
        mockMvc.perform(authed(post("/api/pots"), alice).content(hideParticipant))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- utilitaires

    private User register(String firstName) throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"name":"%s","surname":"Test","email":"%s","password":"motdepasse123","birthday":"2000-01-01"}
                """.formatted(firstName, email);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.token");
        Number id = JsonPath.read(response, "$.roommate.id");
        return new User(token, id.longValue());
    }

    /** Crée une colocation pour l'utilisateur et renvoie son code d'invitation. */
    private String createHouse(User user) throws Exception {
        String body = """
                {"name":"Coloc de test","address":"1 rue du Test","description":"Créée par un test"}
                """;
        String response = mockMvc.perform(authed(post("/api/shared-houses"), user).content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.invitationCode");
    }

    private void join(User user, String invitationCode) throws Exception {
        mockMvc.perform(authed(post("/api/shared-houses/join"), user)
                        .content("{\"invitationCode\":\"" + invitationCode + "\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    /** Crée une cagnotte dont Alice (créatrice) et un autre membre sont participants. */
    private int createPot(User creator, double target, User other) throws Exception {
        String body = """
                {"name":"Cagnotte de test","type":"MATERIEL","targetAmount":%s,"deadline":null,
                 "participantIds":[%d,%d]}
                """.formatted(target, creator.id(), other.id());
        String response = mockMvc.perform(authed(post("/api/pots"), creator).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String payment(double amount) {
        return """
                {"amount":%s,"method":"VIREMENT","paymentDate":"%s"}
                """.formatted(amount, LocalDate.now());
    }

    private double balanceOf(String balancesJson, User user) {
        List<Number> amounts = JsonPath.read(balancesJson, "$.balances[?(@.roommateId == " + user.id() + ")].amount");
        return amounts.get(0).doubleValue();
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder request, User user) {
        return request.header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON);
    }
}
