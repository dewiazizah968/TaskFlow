# TaskFlow

TaskFlow is a web-based project and task management application built with **Spring Boot**. It lets a user create projects, invite members, break work down into tasks with statuses/priorities/deadlines, attach files or links to tasks, chat inside a project, get notifications, view a calendar of deadlines, and export project/task data as PDF reports.

This is a personal / portfolio project built to practice a full-stack Spring Boot setup end to end: server-rendered pages with Thymeleaf, a REST API layer consumed by JavaScript for dynamic parts of the UI, Spring Security (including Google OAuth2 login), a managed cloud database, cloud file storage, and a real production deployment.

**Live demo:** the app is deployed and publicly reachable — see [Deployment](#deployment) below for how, and note the trial/limitation caveat there before relying on the live link long-term.

## Features

- **Authentication**
  - Classic email/password registration and login.
  - "Continue with Google" (OAuth2) login. New Google users are routed through a set-password step so they also have a normal password for the account.
  - Secure, single-use, time-limited password reset tokens, emailed via Brevo.
- **Projects & members**
  - Create, edit, and delete projects.
  - Add/remove members to a project.
  - Per-project dashboard with progress summary.
- **Tasks**
  - Create, edit, delete tasks with status (`TODO`, `IN_PROGRESS`, `DONE`), priority (`LOW`, `MEDIUM`, `HIGH`), and deadline.
  - Filter tasks by status, priority, or assigned user.
  - Task history/audit trail of changes.
  - Attach files (uploaded to Cloudinary) or external links to a task.
- **Collaboration**
  - Lightweight chat per project — automatically relabels itself as a personal "Memo" space when a project has no members besides its owner.
  - Notifications (e.g. upcoming deadlines) with a "mark as read" endpoint.
- **Calendar** view of task deadlines.
- **Search** across the app's data.
- **Reports**
  - Per-project PDF report.
  - Full personal data export (PDF) for a user.
- **Profile**
  - Update profile info, change password, upload a profile picture (stored on Cloudinary).
- **Theme** toggle (light/dark), persisted per user.

## Tech stack

| Layer            | Technology |
|-------------------|------------|
| Language           | Java 17 |
| Framework          | Spring Boot 3.5.14 |
| Security           | Spring Security (form login + OAuth2 login), `thymeleaf-extras-springsecurity6` |
| Web / templating   | Spring Web MVC, Thymeleaf, vanilla JS/CSS for interactive parts |
| Persistence        | Spring Data JPA + PostgreSQL, hosted on **[Neon](https://neon.tech)** (serverless Postgres) |
| File storage        | **[Cloudinary](https://cloudinary.com)** (task attachments + profile pictures — the app itself is stateless, no local disk storage) |
| Validation         | Spring Boot Validation (Jakarta Bean Validation) |
| Email              | **Brevo Transactional Email HTTPS API** (not SMTP — see [Deployment](#deployment) for why) |
| PDF generation      | OpenPDF 1.3.39 (`com.lowagie.text`) |
| Build tool          | Maven |
| Containerization    | Docker (multi-stage build) |
| Hosting             | Deployed from GitHub via a Docker-based PaaS — see [Deployment](#deployment) |

## Project structure

```
src/main/java/com/taskflow/
├── config/         # Spring Security configuration, Cloudinary client bean
├── controller/      # MVC page controllers + REST (@RestController-style) API controllers
├── dto/             # Request/response payloads for the API
├── entity/           # JPA entities (User, Project, Task, ProjectMember, ProjectMessage, TaskFile, Notification, TaskHistory, PasswordResetToken, ...)
├── exception/        # Centralized exception handling
├── repository/       # Spring Data JPA repositories
├── security/          # CustomUserDetailsService, CustomUserDetails, OAuth2LoginSuccessHandler
├── service/           # Business logic (auth, projects, tasks, chat, notifications, email, PDF reports, Cloudinary uploads, ...)
└── util/              # Shared helpers (PDF report styling)

src/main/resources/
├── static/            # CSS, JS, images
├── templates/          # Thymeleaf pages + reusable fragments (sidebar, modals, footer, ...)
└── fonts/               # Fonts embedded into generated PDF reports
```

## How it works

### Authentication flow

- **Email/password**: `POST /api/auth/register` creates a user with a BCrypt-hashed password; `POST /api/auth/login` and Spring Security's form login handle sign-in.
- **Google OAuth2**: Spring Security's `oauth2Login` hands control to `OAuth2LoginSuccessHandler` once Google confirms the user's identity.
  - If a user with that email already exists, their account is linked to the Google ID (if not already) and the session proceeds straight to `/dashboard`.
  - If it's a brand-new Google user, the handler clears the OAuth2-authenticated session (so the person can't wander into the app half-authenticated), stashes the verified Google profile in a fresh session, and redirects to `/register/set-password` where the person chooses a password to finish creating their local account (`POST /api/auth/register-google`).
- **Forgot password**: `POST /api/auth/forgot-password` generates a single-use UUID token with an expiry (`PasswordResetToken`), and emails a reset link via Brevo's HTTPS API. `POST /api/auth/reset-password` validates the token (must exist, be unused, and not expired) before letting the user set a new password; the token is then marked used so the link can't be replayed.
- Public/unauthenticated routes (`/`, `/login`, `/register`, static assets, OAuth2 endpoints, etc.) are configured in `SecurityConfig`; everything else requires an authenticated session.

### File uploads

Task attachments and profile pictures are uploaded straight to **Cloudinary** (via `CloudinaryService`) rather than written to local disk. The app stores Cloudinary's `secure_url` (to display/download the file) and `public_id` (to delete it later) in the database. "Downloading" a file is just an HTTP redirect to its Cloudinary URL. This keeps the app fully stateless, so it can run on hosts with ephemeral/no persistent storage without losing uploaded files on every restart or redeploy.

### Application flow

1. A logged-in user lands on `/dashboard`, which summarizes their projects and tasks.
2. From `/projects`, they can create a project, add members, and open a project's detail page.
3. Inside a project, tasks can be created/edited/moved through statuses, with files/links attached and a history trail kept per task.
4. Project members can chat in real time-ish via polling endpoints (`/api/projects/{id}/chat/messages` and `/messages/after/{id}`).
5. Notifications are generated (e.g. for approaching deadlines) and surfaced via `/api/notifications/me`.
6. `/calendar` renders all of a user's task deadlines.
7. Reports can be downloaded as PDF, either scoped to one project (`/api/projects/{id}/report`) or as a full personal data export from the dashboard (`/dashboard/export`).

## Getting started (running locally)

### Prerequisites

- JDK 17
- Maven (or just use the bundled `./mvnw` / `mvnw.cmd`)
- A [Neon](https://neon.tech) Postgres database (free tier) — or any PostgreSQL instance, local or remote
- A [Cloudinary](https://cloudinary.com) account (free tier) for file uploads
- A [Brevo](https://www.brevo.com) account (free tier) with an **API key** (not SMTP credentials) for the forgot-password email flow
- A Google Cloud OAuth2 Client ID/Secret (only needed if you want to test "Continue with Google")

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/taskflow.git
cd taskflow
```

### 2. Create your local configuration

The real `application.properties` is **not** committed to this repository (it holds database credentials, OAuth2 secrets, and API keys). A sanitized template is provided at [`application.properties.example`](./application.properties.example) — copy it into place and fill in your own values:

```bash
cp application.properties.example src/main/resources/application.properties
```

```properties
spring.application.name=taskflow

# --- PostgreSQL (e.g. a Neon connection string) ---
spring.datasource.url=jdbc:postgresql://<your-neon-host>:5432/<your-db>?sslmode=require
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.thymeleaf.cache=false

spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# --- Cloudinary (file uploads: task attachments + profile pictures) ---
# Get these from your Cloudinary dashboard -> Product Environment Credentials
cloudinary.cloud-name=YOUR_CLOUDINARY_CLOUD_NAME
cloudinary.api-key=YOUR_CLOUDINARY_API_KEY
cloudinary.api-secret=YOUR_CLOUDINARY_API_SECRET

# --- Google OAuth2 login ("Continue with Google") ---
# Create credentials at https://console.cloud.google.com/apis/credentials
# Authorized redirect URI: http://localhost:8080/login/oauth2/code/google
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.provider.google.user-name-attribute=email

# --- Password reset emails ("Forgot password") ---
app.base-url=http://localhost:8080
app.password-reset.expiry-minutes=30

# --- Brevo Transactional Email HTTPS API (not SMTP) ---
# Get this from Brevo dashboard -> SMTP & API -> API Keys (different from the SMTP login/key)
brevo.api-key=YOUR_BREVO_API_KEY
app.mail.sender-name=TaskFlow
app.mail.from-address=your-sender@yourdomain.com
```

> Adjust values to match your own Neon database, Cloudinary account, Google Cloud OAuth2 credentials, and Brevo account. None of the real `application.properties` should ever be pushed to GitHub; it's excluded via `.gitignore`.

### 3. Run the app

Using the Maven wrapper:

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Or build a jar and run it:

```bash
./mvnw clean package
java -jar target/taskflow-*.jar
```

Tables are created/updated automatically on startup via `spring.jpa.hibernate.ddl-auto=update` — no manual `CREATE DATABASE`/migration step needed beyond having an empty Postgres database to point at.

### 4. Open it in a browser

```
http://localhost:8080
```

Register a new account (or sign in with Google, if configured) and start creating projects.

## Deployment

The app ships with a multi-stage `Dockerfile` (Maven build → slim JRE runtime image), so it can be deployed to any Docker-friendly host directly from this GitHub repository.

It's currently deployed on **[Railway](https://railway.com)**, connected straight to this repo — every push to `main` triggers a fresh build and deploy. Configuration (database URL, Cloudinary keys, Google OAuth2 credentials, Brevo API key, etc.) is supplied as a single `SPRING_APPLICATION_JSON` environment variable, which Spring Boot parses natively at startup — this avoids translating every `application.properties` key into the platform's environment-variable naming rules by hand.

Two host-specific things worth knowing if you redeploy this elsewhere:

- **`server.forward-headers-strategy=framework`** is set so Spring Security correctly builds `https://` redirect URLs (e.g. for Google OAuth2) even though the platform's proxy forwards requests to the container over plain HTTP internally. Without this, OAuth2 login fails with a `redirect_uri_mismatch` because the generated redirect URI comes out as `http://` instead of `https://`.
- **Email is sent via Brevo's HTTPS API, not SMTP.** Several free/trial hosting tiers (Railway's among them) block outbound SMTP ports (25/465/587) entirely to prevent spam abuse, which silently breaks any `JavaMailSender`/SMTP-based email code. Using Brevo's plain HTTPS API instead sidesteps this completely, and works identically on any host.

## Notes

- File uploads (task attachments, profile pictures) live on Cloudinary, not on local disk — this keeps the app stateless, which matters because most affordable hosting tiers don't offer persistent local storage across restarts/redeploys.
- PDF reports are generated with OpenPDF and embed the DejaVu Sans fonts bundled under `src/main/resources/fonts` so they render correctly regardless of the OS running the app.
