# WW2 AI Service

Production-ready Spring Boot application with PostgreSQL database and AI integration.

## Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Git

### Run the application
```bash
# Clone the repository
git clone <repository-url>
cd ww2

# Start the application with database
docker-compose up -d

# Check logs
docker-compose logs -f ww2-app
```

### Access the application
- **API**: http://localhost:8081
- **Health Check**: http://localhost:8081/actuator/health

## API Usage

### Process Question
```bash
curl -X POST http://localhost:8081/api/question \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How to apply for a credit card?",
    "category": "Banking"
  }'
```

### Available Categories
- `Banking` - Banking services (credit cards, loans, savings)
- `Technical Support` - Technical issues (login, mobile app)
- `General` - General inquiries (contact, support)

## Production Configuration

The application uses environment variables for configuration:

- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `SPRING_AI_OPENAI_API_KEY` - AI service API key
- `SPRING_AI_OPENAI_BASE_URL` - AI service base URL

## Management

### Stop the application
```bash
docker-compose down
```

### View logs
```bash
docker-compose logs -f
```

### Restart services
```bash
docker-compose restart
```

## Architecture

- **Application**: Spring Boot with WebFlux for reactive programming
- **Database**: PostgreSQL 16 with persistent volumes
- **AI Integration**: SciBox AI service for intelligent responses
- **Health Monitoring**: Spring Boot Actuator for health checks
- **Containerization**: Multi-stage Docker build for optimized production image