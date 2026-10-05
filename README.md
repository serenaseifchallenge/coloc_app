# coloc_app


```bash
# Terminal ouvert depuis dossier général
cp .env.example .env # Editer .env : mettez votre nom de BDD, nom de user et mdp
docker compose up -d

# Terminal ouvert depuis dossier back/
./mvnw spring-boot:run

# Terminal ouvert depuis dossier front/
gn serve
```