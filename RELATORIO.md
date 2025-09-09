# Projeto ReviewApp

**Aluno:** Tiago Daniel Moreira Andrade
**Nºmecanográfico:** 8190563
**Curso:** Licenciatura em Engenharia Informática
**Disciplina:** Computação Móvel e Ubíqua
**Data de entrega:** 09/09/2025
-------------------------------

# 1. Descrição do Projeto

O presente projeto foi desenvolvido no âmbito da unidade curricular **Computação Móvel e Ubíqua** com o objetivo de consolidar conhecimentos em **Android**, **Kotlin**, **Jetpack Compose**, **arquitetura MVVM**, **Hilt** e integração de **APIs/serviços Google & Firebase**.

A aplicação — **ReviewApp** — centra-se em **cafés e pastelarias**, permitindo ao utilizador descobrir locais próximos, consultar detalhes e **submeter avaliações** com foto, de forma simples e fiável. O sistema inclui **geofences** para notificações de proximidade, **cache local (Room)** para robustez offline e **integração com Google Places** para enriquecer metadados.

## Funcionalidades

1. **Autenticação e registo com username único** – criação de conta e login via Firebase Auth; reserva de `username` único em Firestore.
2. **Pesquisa de estabelecimentos próximos** – resultados por lista e **mapa interativo** (Google Maps Compose), com geocodificação de texto para coordenadas.
3. **Submissão de reviews** – com **regras de proximidade** e de intervalo temporal:

   * distância: regra atual do formulário configurada para **≤ 250 m** (parametrizável em `ReviewRules.MIN_DISTANCE_METERS`);
   * intervalo: **≥ 30 min** entre avaliações do mesmo utilizador;
   * campos: “Doçaria”, “Rating (1–5)”, “Comentário”; **foto opcional** (câmara/galeria).
4. **Leaderboard** – rankings de **estabelecimentos** e de **doçarias**, agregados a partir das reviews internas, com enriquecimento de nomes/moradas via repositório de locais.
5. **Histórico do utilizador** – lista cronológica das reviews do próprio (com miniatura, rating, data).
6. **Consulta de reviews** – ecrãs para listar e filtrar reviews (ordenação, “apenas com foto”, “apenas minhas”, mínimo de estrelas).
7. **Notificações geolocalizadas (Geofence)** – ao **entrar ou permanecer** numa área de **raio 50 m** de um local, é enviada notificação **apenas entre as 16h e as 18h** (janela temporal aplicada em runtime).
8. **Detalhes do estabelecimento** – mostra categoria/morada, **ratings Google** e **média interna**, ações de chamada e coordenadas (componente de mapa disponível no ecrã de Pesquisa).
9. **Experiência offline** – **Room** como cache local e **banner de offline** reativo (mostrar estado de conectividade).

A aplicação foi concebida para uma **experiência fluida, resiliente a falhas de rede e energeticamente consciente**, respeitando boas práticas da plataforma.

# 2. Funcionalidades Implementadas

## 1. Autenticação e registo

* Login/Logout e registo com `username` único (coleções `users`/`usernames` no Firestore).
* Atualização de `displayName` no perfil do utilizador.

## 2. Descoberta de locais próximos

* **Google Places (Nearby Search + Details)** via Retrofit.
* **Filtro por raio** (250 m · 1 km · 3 km · 5 km) e **ordenação** (rating Google, nº de ratings, distância).

## 3. Submissão de reviews com regras

* Regras em `ReviewRules`: **distância (≤ 250 m)** e **intervalo (≥ 30 min)**.
* Captura/seleção de **foto** (MediaStore + permissões), persistência local e *sync* posterior (worker).
* Validações e mensagens contextualizadas no UI (Compose).

## 4. Leaderboard

* Agregação de médias/contagens por **estabelecimento** e por **doçaria** (top ordenado).
* Enriquecimento assíncrono com dados do repositório de locais.

## 5. Histórico

* Ecrã “Histórico” com as reviews do utilizador, ordenadas por data, com imagem/rating.

## 6. Consulta e filtros de reviews

* Ecrãs “Detalhes” (top 10 filtrado) e “Todas as Reviews” com filtros/ordenação configuráveis.

## 7. Notificações geofence (16h–18h)

* Registo/refresh de **geofences** (ENTER/DWELL, raio **50 m**).
* **Entrega condicionada à janela 16–18** (TimeUtils) e às permissões de **notificações** e **localização em background**.
* **Tile custom** com branding light/dark.

## 8. Detalhes do estabelecimento

* Categoria, morada, contacto (ação “Ligar”), lat/lng, **rating Google** e **média interna** das reviews.

## 9. Robustez offline

* **Room** para cache de locais e reviews; **banner** de conectividade e fluxo de leitura tolerante a falhas.

# 3. Decisões de Implementação

## 1. Room

**Room** serve de cache local para garantir continuidade **offline** e reduzir latência/tráfego. DAO/entidades refletem o modelo de domínio (locais e reviews).

## 2. Firebase (Auth, Firestore e Storage)

* **Auth** para credenciais.
* **Firestore** para perfis, reviews e metadados (streams e *queries* paginadas/compostas).
* **Storage** para fotos das reviews (suportado por *worker* de sincronização).

## 3. Jetpack Compose + Material 3

UI declarativa, **componentes reutilizáveis** (headers, cards, filtros) e **tema** com cores/typography próprias, incluindo **Dynamic Color** (Android 12+).

## 4. Hilt

**Injeção de dependências** (repositórios, DAOs, clientes Google). Facilita testabilidade e modularidade.

## 5. Retrofit + Google Places

**Retrofit** para Nearby/Details; *mappers* convertem respostas em modelos internos e entidades Room.

## 6. MVVM + Coroutines/Flow

**ViewModels** desacoplam UI/lógica de dados; **Flow** para *streams* (online/offline) e **coroutines** para IO e concorrência controlada.

## 7. Google Maps Compose

Mapa interativo na **Pesquisa**, com *markers* e “Pesquisar aqui”. **Geocoder** para transformar texto em coordenadas.

## 8. Geofence

**GeofencingClient** + **SettingsClient**; *receivers* dedicados a transições e a atualizações de localização com *PendingIntent*.

# 4. Resultados Obtidos

## 4.1 Funcionalidades implementadas

* Autenticação/Registo com username único.
* Descoberta de locais próximos (lista + mapa; filtro de raio; ordenação).
* Submissão de reviews com validações (distância ≤ 250 m; intervalo ≥ 30 min; foto opcional).
* Leaderboard (estabelecimentos e doçarias) com enriquecimento de dados.
* Histórico do utilizador e consulta de reviews.
* **Notificações de proximidade** (geofence **50 m**) **entre as 16h e as 18h**.
* Detalhes do estabelecimento (ratings Google + internos, contacto).
* **Cache offline** e UI reativa ao estado de rede.

## 4.2 Limitações e observações

* **Inconsistência intencional 50 m vs 250 m**: geofence usa **50 m** para **notificar**; o formulário usa **250 m** para **permitir avaliar** — opção de UX para reduzir falsos negativos na utilização real. Pode ser unificado alterando `ReviewRules.MIN_DISTANCE_METERS`.
* A precisão/latência de localização varia por dispositivo e condições (GPS/Network), impactando geofence e cálculos de distância.
* Em modo de **poupança de energia**/restrições de background, o sistema pode adiar geofences e *workers*.
* O *tile* de notificação é informativo; ações “deep link” podem ser adicionadas como melhoria.
* Dependência de **Google Places**: sujeita a limites de quota e à presença de **API key**.

# 5. Conclusão

O projeto permitiu consolidar práticas modernas de **desenvolvimento Android**: UI declarativa, integração com serviços externos, arquitetura limpa e **context awareness** (geolocalização/tempo). A aplicação está funcional, com experiência robusta e responsiva.

## 5.1 Aprendizagens

* **Kotlin + Compose** (estado, composição, Material 3).
* **MVVM + Flow/Coroutines** e **Hilt** para DI.
* Integração com **Firebase** (Auth/Firestore/Storage).
* Consumo da **Google Places API** com Retrofit.
* **Geofence** e notificações condicionadas por **janela temporal (16–18h)**.
* **Room** e estratégias de **offline-first**.

## 5.2 Pontos fortes

* Código modular e documentado (KDoc/Dokka).
* Experiência consistente **online/offline**.
* Notificações contextuais com branding e restrição temporal.

# 6. Documentação

A base de código está comentada com **KDoc** e a documentação pode ser gerada com **Dokka**:

```bash
./gradlew dokkaHtml
```

Os artefactos HTML ficam em `build/dokka/html`.

---
