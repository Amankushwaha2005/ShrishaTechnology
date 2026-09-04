# Shrisha Technology Website

A full-stack web application built with **Java (Spring Boot)** and **PostgreSQL**, with server-rendered pages (Jinja2 templates), user authentication, admin dashboard, and Razorpay payments.

## 🌟 Features

- **User Authentication**: Secure login/registration system with bcrypt password hashing
- **Session Management**: HTTP session-based user session handling
- **Database**: PostgreSQL for persistent data storage
- **Template Engine**: Jinja2 for dynamic server-side rendering
- **Admin Tools**: Grant admin privileges to users via command-line scripts
- **Payments**: Razorpay advance + balance payment flow

## 🛠️ Tech Stack

| Technology | Purpose | Version |
|------------|---------|---------|
| **Java** | Runtime | 21+ |
| **Spring Boot** | Web framework | 3.4 |
| **Jinjava** | Template engine | 2.7 |
| **PostgreSQL** | Database | 14+ |
| **JDBC / HikariCP** | PostgreSQL client | — |
| **BCrypt** | Password hashing | Spring Security Crypto |

### Language Composition
- **JavaScript**: 36.9%
- **HTML**: 25.7%
- **Templates**: Jinja2
- **CSS**: 15.8%
- **Batchfile**: 0.2%

## 📋 Prerequisites

- Java 21+ (local)
- Apache Maven 3.9+ (or the portable copy in `.tools/`)
- PostgreSQL 14+ (local)

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Amankushwaha2005/techwithaman-website.git shrisha-technology
   cd shrisha-technology
   ```

2. **Install Java 21+ and Maven**, then from the project root:
   ```bash
   mvn -f backend/pom.xml spring-boot:run
   ```
   Or double-click `START-SITE.bat`.

3. **Install and start PostgreSQL**, then create a database:
   ```bash
   createdb web_project
   ```
   (Or use pgAdmin / psql: `CREATE DATABASE web_project;`)

4. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Set DATABASE_URL or PGHOST, PGUSER, PGPASSWORD, PGDATABASE in .env
   ```

5. **Start the application**
   ```bash
   mvn -f backend/pom.xml spring-boot:run
   ```

The server will start and be available at `http://localhost:3000` (or your configured port).

## 📚 Available Scripts

| Script | Command | Description |
|--------|---------|-------------|
| **start** | `npm start` | Start the production server |
| **dev** | `npm run dev` | Run the development server |
| **grant-admin** | `npm run grant-admin` | Promote a user to admin status |
| **sync-pages** | `npm run sync-pages` | Synchronize HTML files to EJS templates |
| **test** | `npm test` | Run test suite (not yet configured) |

## 📁 Project Structure

```
shrisha-technology/
├── server.js              # Main application entry point
├── package.json           # Project dependencies and scripts
├── public/                # Static assets (CSS, images, client-side JS)
├── views/                 # EJS template files
├── routes/                # Express route handlers
├── controllers/           # Business logic
├── middleware/            # Custom middleware
├── scripts/               # Utility scripts
│   ├── grant-admin.js     # Admin privilege script
│   └── sync-html-to-ejs.js # HTML to EJS converter
├── data/                  # Local runtime/generated files (optional)
├── .env.example           # Environment variables template
└── README.md              # This file
```

## 🔐 Security Features

- **Password Hashing**: Uses bcryptjs for secure password storage
- **Session Management**: Express session middleware for secure user sessions
- **Environment Configuration**: Sensitive data managed through environment variables

## 🗄️ Database

The application uses **PostgreSQL**:
- **Client**: `pg` (node-postgres)
- **Config**: `DATABASE_URL` or `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE` in `.env`
# Schema is created automatically when the Java server starts.
- **GUI**: pgAdmin, DBeaver, or any PostgreSQL client (not MySQL Workbench)

## 🔧 Configuration

Create a `.env` file in the root directory with the following variables:

```env
# Server Configuration
PORT=3000
NODE_ENV=development

# Session Configuration
SESSION_SECRET=your_secret_key_here

# PostgreSQL
DATABASE_URL=postgres://postgres:password@127.0.0.1:5432/web_project

# Other configurations
```

## 🧑‍💻 Usage Examples

### Starting the Server
```bash
npm start
```

### Granting Admin Access
```bash
npm run grant-admin
```

### Syncing HTML Templates to EJS
```bash
npm run sync-pages
```

## 📝 License

This project is licensed under the ISC License. See the LICENSE file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Support

For issues and questions, please visit the [Issues page](https://github.com/Amankushwaha2005/techwithaman-website/issues).

## 📦 Repository Information

- **Repository**: [Amankushwaha2005/techwithaman-website](https://github.com/Amankushwaha2005/techwithaman-website)
- **Language**: JavaScript
- **Node Version Required**: 20.x
- **Package Type**: CommonJS

---

**Happy Coding! 🚀**
