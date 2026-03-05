# Query Builder API

A Spring Boot REST API backend for a query builder application.

## Features

- User management
- Variable management
- PostgreSQL database integration
- CORS support
- Security with Spring Security (HTTP Basic Auth)
- Input validation
- Rate limiting
- HTTPS support (configurable)

## Prerequisites

- Java 21
- Maven 3.6+
- PostgreSQL (or Docker for containerized deployment)

## Local Development Setup

1. Clone the repository
2. Set up PostgreSQL database
3. Configure environment variables or update `.env` file:
   ```
   export DB_URL="jdbc:postgresql://localhost:5432/your_db"
   export DB_USERNAME="your_username"
   export DB_PASSWORD="your_password"
   export API_USERNAME="admin"
   export API_PASSWORD="your_secure_password"
   ```
4. Run `mvn spring-boot:run`

## Production Deployment

### Using Docker Compose (Recommended)

1. Update the environment variables in `docker-compose.yml`:
   ```yaml
   environment:
     POSTGRES_PASSWORD: your_secure_db_password
     API_PASSWORD: your_secure_api_password
   ```

2. Run the application:
   ```bash
   docker-compose up -d
   ```

3. The application will be available at:
   - Frontend: http://localhost
   - Backend API: http://localhost:8080/api/

### Manual Deployment

1. Set environment variables:
   ```bash
   export DB_URL="jdbc:postgresql://your_host:5432/your_db"
   export DB_USERNAME="your_username"
   export DB_PASSWORD="your_password"
   export API_USERNAME="admin"
   export API_PASSWORD="your_secure_password"
   ```

2. Build and run:
   ```bash
   mvn clean package
   java -jar target/*.jar
   ```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/query_builder_db` | PostgreSQL connection URL |
| `DB_USERNAME` | `your_username` | Database username |
| `DB_PASSWORD` | `your_password` | Database password |
| `API_USERNAME` | `admin` | API basic auth username |
| `API_PASSWORD` | `change_this_password_in_production` | API basic auth password |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Allowed CORS origins |

## API Endpoints

- `GET /api/users` - Get all users (requires authentication)
- `GET /api/users/{id}` - Get user by ID (requires authentication)
- `GET /api/variables` - Get all variables (requires authentication)
- `GET /api/variables/{id}` - Get variable by ID (requires authentication)

## Security

- **Authentication**: HTTP Basic Auth with configurable credentials
- **Authorization**: All /api/** endpoints require authentication
- **Rate Limiting**: 100 requests per minute per IP
- **Input Validation**: Bean validation on models
- **HTTPS**: Configurable via application.properties

Database credentials and API credentials are externalized via environment variables. Never commit sensitive data to version control.