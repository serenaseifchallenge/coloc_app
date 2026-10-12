package com.coloc.back.shopping;

import com.coloc.back.dto.ArticleResponse;
import com.coloc.back.dto.CreateArticlesRequest;
import com.coloc.back.dto.ShoppingListType;
import com.coloc.back.dto.UpdateArticleRequest;
import com.coloc.back.entity.Article;
import com.coloc.back.entity.Roommate;
import com.coloc.back.entity.SharedHouse;
import com.coloc.back.repository.ArticleRepository;
import com.coloc.back.service.ArticleService;
import com.coloc.back.service.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    private static final Long HOUSE_ID = 1L;
    private static final Long ME_ID = 5L;
    private static final Long OTHER_ROOMMATE_ID = 7L;
    private static final Long ARTICLE_ID = 20L;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ArticleService articleService;

    @Captor
    private ArgumentCaptor<List<Article>> articlesCaptor;


    @Nested
    @DisplayName("getArticles")
    class GetArticles {

        @Test
        @DisplayName("liste perso : seulement mes articles, dans ma coloc")
        void returnsMyPersonalArticles() {
            SharedHouse house = house();
            Article milk = article(house, roommate(ME_ID, house), "Lait", false);
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(currentUserService.getCurrentRoommateId()).thenReturn(ME_ID);
            when(articleRepository.findBySharedHouseIdAndOwnerIdOrderByIdAsc(HOUSE_ID, ME_ID))
                    .thenReturn(List.of(milk));

            List<ArticleResponse> result = articleService.getArticles(ShoppingListType.PERSONAL);

            assertThat(result).containsExactly(new ArticleResponse(ARTICLE_ID, "Lait", false));
            verify(articleRepository, never()).findBySharedHouseIdAndOwnerIsNullOrderByIdAsc(any());
        }

        @Test
        @DisplayName("liste commune : les articles sans propriétaire de ma coloc")
        void returnsSharedArticles() {
            SharedHouse house = house();
            Article soap = article(house, null, "Liquide vaisselle", true);
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(articleRepository.findBySharedHouseIdAndOwnerIsNullOrderByIdAsc(HOUSE_ID))
                    .thenReturn(List.of(soap));

            List<ArticleResponse> result = articleService.getArticles(ShoppingListType.SHARED);

            assertThat(result).containsExactly(new ArticleResponse(ARTICLE_ID, "Liquide vaisselle", true));
            verify(currentUserService, never()).getCurrentRoommateId();
        }
    }


    @Nested
    @DisplayName("createArticles")
    class CreateArticles {

        @Test
        @DisplayName("liste perso : articles à moi, noms nettoyés, non achetés, dans l'ordre envoyé")
        void createsPersonalArticlesOwnedByMe() {
            SharedHouse house = house();
            Roommate me = roommate(ME_ID, house);
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(currentUserService.getCurrentRoommate()).thenReturn(me);
            when(articleRepository.saveAll(ArgumentMatchers.<Article>anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            List<ArticleResponse> result = articleService.createArticles(
                    new CreateArticlesRequest(ShoppingListType.PERSONAL, List.of("  Lait ", "Pain")));

            verify(articleRepository).saveAll(articlesCaptor.capture());
            List<Article> saved = articlesCaptor.getValue();
            assertThat(saved).extracting(Article::getName).containsExactly("Lait", "Pain");
            assertThat(saved).allSatisfy(article -> {
                assertThat(article.getOwner()).isSameAs(me);
                assertThat(article.getSharedHouse()).isSameAs(house);
                assertThat(article.isBought()).isFalse();
            });
            assertThat(result).extracting(ArticleResponse::name).containsExactly("Lait", "Pain");
        }

        @Test
        @DisplayName("liste commune : articles sans propriétaire")
        void createsSharedArticlesWithoutOwner() {
            SharedHouse house = house();
            when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
            when(articleRepository.saveAll(ArgumentMatchers.<Article>anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            articleService.createArticles(new CreateArticlesRequest(ShoppingListType.SHARED, List.of("Éponges")));

            verify(articleRepository).saveAll(articlesCaptor.capture());
            Article saved = articlesCaptor.getValue().get(0);
            assertThat(saved.getOwner()).isNull();
            assertThat(saved.getName()).isEqualTo("Éponges");
            verify(currentUserService, never()).getCurrentRoommate();
        }
    }


    @Nested
    @DisplayName("updateArticle")
    class UpdateArticle {

        @Test
        @DisplayName("article commun non acheté : renommé (nom nettoyé)")
        void renamesSharedArticle() {
            SharedHouse house = house();
            Article article = article(house, null, "Lait", false);
            givenArticleFound(house, article);

            ArticleResponse result = articleService.updateArticle(ARTICLE_ID, new UpdateArticleRequest("  Lait d'avoine "));

            assertThat(article.getName()).isEqualTo("Lait d'avoine");
            assertThat(result.name()).isEqualTo("Lait d'avoine");
        }

        @Test
        @DisplayName("mon article perso : renommé")
        void renamesMyPersonalArticle() {
            SharedHouse house = house();
            Article article = article(house, roommate(ME_ID, house), "Pain", false);
            givenArticleFound(house, article);

            articleService.updateArticle(ARTICLE_ID, new UpdateArticleRequest("Baguette"));

            assertThat(article.getName()).isEqualTo("Baguette");
        }

        @Test
        @DisplayName("article déjà acheté : 409 et le nom ne change pas")
        void rejectsBoughtArticle() {
            SharedHouse house = house();
            Article article = article(house, null, "Lait", true);
            givenArticleFound(house, article);

            assertStatus(() -> articleService.updateArticle(ARTICLE_ID, new UpdateArticleRequest("Beurre")),
                    HttpStatus.CONFLICT);
            assertThat(article.getName()).isEqualTo("Lait");
        }

        @Test
        @DisplayName("article perso d'un autre colocataire : 404 (on ne révèle pas qu'il existe)")
        void rejectsPersonalArticleOfSomeoneElse() {
            SharedHouse house = house();
            Article article = article(house, roommate(OTHER_ROOMMATE_ID, house), "Chocolat", false);
            givenArticleFound(house, article);

            assertStatus(() -> articleService.updateArticle(ARTICLE_ID, new UpdateArticleRequest("Bonbons")),
                    HttpStatus.NOT_FOUND);
            assertThat(article.getName()).isEqualTo("Chocolat");
        }

        @Test
        @DisplayName("article introuvable (ou d'une autre coloc) : 404")
        void rejectsUnknownArticle() {
            givenArticleNotFound();

            assertStatus(() -> articleService.updateArticle(ARTICLE_ID, new UpdateArticleRequest("Beurre")),
                    HttpStatus.NOT_FOUND);
        }
    }


    @Nested
    @DisplayName("buyArticle / unbuyArticle")
    class BuyAndUnbuy {

        @Test
        @DisplayName("buyArticle : l'article passe à acheté")
        void marksArticleAsBought() {
            SharedHouse house = house();
            Article article = article(house, null, "Lait", false);
            givenArticleFound(house, article);

            articleService.buyArticle(ARTICLE_ID);

            assertThat(article.isBought()).isTrue();
        }

        @Test
        @DisplayName("unbuyArticle : l'article repasse à non acheté")
        void marksArticleAsNotBought() {
            SharedHouse house = house();
            Article article = article(house, roommate(ME_ID, house), "Lait", true);
            givenArticleFound(house, article);

            articleService.unbuyArticle(ARTICLE_ID);

            assertThat(article.isBought()).isFalse();
        }

        @Test
        @DisplayName("buyArticle sur l'article perso d'un autre : 404 et rien ne change")
        void rejectsBuyingSomeoneElsesPersonalArticle() {
            SharedHouse house = house();
            Article article = article(house, roommate(OTHER_ROOMMATE_ID, house), "Chocolat", false);
            givenArticleFound(house, article);

            assertStatus(() -> articleService.buyArticle(ARTICLE_ID), HttpStatus.NOT_FOUND);
            assertThat(article.isBought()).isFalse();
        }

        @Test
        @DisplayName("unbuyArticle sur un article introuvable : 404")
        void rejectsUnbuyingUnknownArticle() {
            givenArticleNotFound();

            assertStatus(() -> articleService.unbuyArticle(ARTICLE_ID), HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteArticle")
    class DeleteArticle {

        @Test
        @DisplayName("article accessible : supprimé")
        void deletesAccessibleArticle() {
            SharedHouse house = house();
            Article article = article(house, null, "Lait", false);
            givenArticleFound(house, article);

            articleService.deleteArticle(ARTICLE_ID);

            verify(articleRepository).delete(article);
        }

        @Test
        @DisplayName("article perso d'un autre : 404 et rien n'est supprimé")
        void rejectsDeletingSomeoneElsesPersonalArticle() {
            SharedHouse house = house();
            givenArticleFound(house, article(house, roommate(OTHER_ROOMMATE_ID, house), "Chocolat", false));

            assertStatus(() -> articleService.deleteArticle(ARTICLE_ID), HttpStatus.NOT_FOUND);
            verify(articleRepository, never()).delete(any(Article.class));
        }

        @Test
        @DisplayName("article introuvable : 404")
        void rejectsUnknownArticle() {
            givenArticleNotFound();

            assertStatus(() -> articleService.deleteArticle(ARTICLE_ID), HttpStatus.NOT_FOUND);
            verify(articleRepository, never()).delete(any(Article.class));
        }
    }


    @Test
    @DisplayName("deletePersonalArticlesOf : délègue au repository avec la coloc et le propriétaire")
    void deletePersonalArticlesOfDelegatesToRepository() {
        articleService.deletePersonalArticlesOf(HOUSE_ID, OTHER_ROOMMATE_ID);

        verify(articleRepository).deletePersonalArticles(HOUSE_ID, OTHER_ROOMMATE_ID);
    }


    private void givenArticleFound(SharedHouse house, Article article) {
        when(currentUserService.getCurrentSharedHouse()).thenReturn(house);
        when(currentUserService.getCurrentRoommateId()).thenReturn(ME_ID);
        when(articleRepository.findByIdAndSharedHouseId(ARTICLE_ID, HOUSE_ID)).thenReturn(Optional.of(article));
    }

    private void givenArticleNotFound() {
        when(currentUserService.getCurrentSharedHouse()).thenReturn(house());
        when(currentUserService.getCurrentRoommateId()).thenReturn(ME_ID);
        when(articleRepository.findByIdAndSharedHouseId(ARTICLE_ID, HOUSE_ID)).thenReturn(Optional.empty());
    }

    private static void assertStatus(Runnable action, HttpStatus expectedStatus) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(expectedStatus);
    }

    private static SharedHouse house() {
        SharedHouse house = new SharedHouse();
        house.setId(HOUSE_ID);
        return house;
    }

    private static Roommate roommate(Long id, SharedHouse house) {
        Roommate roommate = new Roommate();
        roommate.setId(id);
        roommate.setSharedHouse(house);
        return roommate;
    }

    private static Article article(SharedHouse house, Roommate owner, String name, boolean bought) {
        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setSharedHouse(house);
        article.setOwner(owner);
        article.setName(name);
        article.setBought(bought);
        return article;
    }
}