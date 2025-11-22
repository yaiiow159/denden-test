# Makefile for Member Auth System Docker Operations

.PHONY: help build up down restart logs clean backup restore test

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(BLUE)Member Auth System - Docker Operations$(NC)"
	@echo ""
	@echo "$(GREEN)Available targets:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2}'

# Development targets
dev: ## Start services in development mode
	@echo "$(GREEN)Starting services in development mode...$(NC)"
	docker-compose up -d
	@echo "$(GREEN)Services started. Access at http://localhost:8080$(NC)"

dev-build: ## Build and start services in development mode
	@echo "$(GREEN)Building and starting services in development mode...$(NC)"
	docker-compose up -d --build
	@echo "$(GREEN)Services started. Access at http://localhost:8080$(NC)"

# Production targets
prod: ## Start services in production mode
	@echo "$(GREEN)Starting services in production mode...$(NC)"
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
	@echo "$(GREEN)Services started in production mode$(NC)"

prod-build: ## Build and start services in production mode
	@echo "$(GREEN)Building and starting services in production mode...$(NC)"
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
	@echo "$(GREEN)Services built and started in production mode$(NC)"

# Service management
build: ## Build all Docker images
	@echo "$(GREEN)Building Docker images...$(NC)"
	docker-compose build

up: ## Start all services
	@echo "$(GREEN)Starting all services...$(NC)"
	docker-compose up -d

down: ## Stop all services
	@echo "$(YELLOW)Stopping all services...$(NC)"
	docker-compose down

restart: ## Restart all services
	@echo "$(YELLOW)Restarting all services...$(NC)"
	docker-compose restart

stop: ## Stop all services without removing containers
	@echo "$(YELLOW)Stopping all services...$(NC)"
	docker-compose stop

start: ## Start stopped services
	@echo "$(GREEN)Starting stopped services...$(NC)"
	docker-compose start

# Individual service management
app-restart: ## Restart application service only
	@echo "$(YELLOW)Restarting application...$(NC)"
	docker-compose restart app

db-restart: ## Restart PostgreSQL service only
	@echo "$(YELLOW)Restarting PostgreSQL...$(NC)"
	docker-compose restart postgres

redis-restart: ## Restart Redis service only
	@echo "$(YELLOW)Restarting Redis...$(NC)"
	docker-compose restart redis

# Logs
logs: ## Show logs from all services
	docker-compose logs -f

logs-app: ## Show logs from application service
	docker-compose logs -f app

logs-db: ## Show logs from PostgreSQL service
	docker-compose logs -f postgres

logs-redis: ## Show logs from Redis service
	docker-compose logs -f redis

# Status and monitoring
ps: ## Show status of all services
	docker-compose ps

stats: ## Show resource usage statistics
	docker stats

health: ## Check health status of all services
	@echo "$(BLUE)Checking service health...$(NC)"
	@docker-compose ps
	@echo ""
	@echo "$(BLUE)Application health:$(NC)"
	@curl -s http://localhost:8080/actuator/health | jq . || echo "$(RED)Application not responding$(NC)"
	@echo ""
	@echo "$(BLUE)PostgreSQL health:$(NC)"
	@docker-compose exec postgres pg_isready -U admin || echo "$(RED)PostgreSQL not ready$(NC)"
	@echo ""
	@echo "$(BLUE)Redis health:$(NC)"
	@docker-compose exec redis redis-cli ping || echo "$(RED)Redis not responding$(NC)"

# Shell access
shell-app: ## Open shell in application container
	docker-compose exec app sh

shell-db: ## Open PostgreSQL shell
	docker-compose exec postgres psql -U admin member_auth

shell-redis: ## Open Redis CLI
	docker-compose exec redis redis-cli

# Database operations
db-backup: ## Backup PostgreSQL database
	@echo "$(GREEN)Creating database backup...$(NC)"
	@mkdir -p backups/postgres
	@docker-compose exec -T postgres pg_dump -U admin member_auth > backups/postgres/backup_$$(date +%Y%m%d_%H%M%S).sql
	@gzip backups/postgres/backup_*.sql 2>/dev/null || true
	@echo "$(GREEN)Backup completed$(NC)"

db-restore: ## Restore PostgreSQL database (usage: make db-restore FILE=backup.sql)
	@if [ -z "$(FILE)" ]; then \
		echo "$(RED)Error: FILE parameter required. Usage: make db-restore FILE=backup.sql$(NC)"; \
		exit 1; \
	fi
	@echo "$(YELLOW)Restoring database from $(FILE)...$(NC)"
	@docker-compose exec -T postgres psql -U admin member_auth < $(FILE)
	@echo "$(GREEN)Database restored$(NC)"

db-migrate: ## Run database migrations
	@echo "$(GREEN)Running database migrations...$(NC)"
	docker-compose exec app ./mvnw flyway:migrate

db-reset: ## Reset database (WARNING: destroys all data)
	@echo "$(RED)WARNING: This will destroy all database data!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose down -v postgres; \
		docker-compose up -d postgres; \
		echo "$(GREEN)Database reset completed$(NC)"; \
	else \
		echo "$(YELLOW)Operation cancelled$(NC)"; \
	fi

# Redis operations
redis-backup: ## Backup Redis data
	@echo "$(GREEN)Creating Redis backup...$(NC)"
	@mkdir -p backups/redis
	@docker-compose exec redis redis-cli SAVE
	@docker cp member-auth-redis:/data/dump.rdb backups/redis/dump_$$(date +%Y%m%d_%H%M%S).rdb
	@echo "$(GREEN)Redis backup completed$(NC)"

redis-flush: ## Flush all Redis data (WARNING: destroys all cache)
	@echo "$(RED)WARNING: This will destroy all Redis cache data!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose exec redis redis-cli FLUSHALL; \
		echo "$(GREEN)Redis flushed$(NC)"; \
	else \
		echo "$(YELLOW)Operation cancelled$(NC)"; \
	fi

# Cleanup operations
clean: ## Stop services and remove containers
	@echo "$(YELLOW)Cleaning up containers...$(NC)"
	docker-compose down

clean-all: ## Stop services and remove containers, volumes, and images
	@echo "$(RED)WARNING: This will remove all containers, volumes, and images!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose down -v --rmi all; \
		echo "$(GREEN)Cleanup completed$(NC)"; \
	else \
		echo "$(YELLOW)Operation cancelled$(NC)"; \
	fi

clean-volumes: ## Remove all volumes (WARNING: destroys all data)
	@echo "$(RED)WARNING: This will destroy all persistent data!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose down -v; \
		echo "$(GREEN)Volumes removed$(NC)"; \
	else \
		echo "$(YELLOW)Operation cancelled$(NC)"; \
	fi

prune: ## Remove unused Docker resources
	@echo "$(YELLOW)Pruning unused Docker resources...$(NC)"
	docker system prune -f
	@echo "$(GREEN)Prune completed$(NC)"

# Testing
test: ## Run tests in Docker container
	@echo "$(GREEN)Running tests...$(NC)"
	docker-compose exec app ./mvnw test

test-integration: ## Run integration tests
	@echo "$(GREEN)Running integration tests...$(NC)"
	docker-compose exec app ./mvnw verify -P integration-test

# Setup and initialization
init: ## Initialize project (create directories, copy env file)
	@echo "$(GREEN)Initializing project...$(NC)"
	@mkdir -p data/postgres data/redis logs backups/postgres backups/redis
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(GREEN).env file created. Please edit it with your configuration.$(NC)"; \
	else \
		echo "$(YELLOW).env file already exists$(NC)"; \
	fi
	@echo "$(GREEN)Initialization completed$(NC)"

setup: init build ## Setup project and build images
	@echo "$(GREEN)Setup completed. Run 'make dev' to start services.$(NC)"

# Documentation
docs: ## Open API documentation in browser
	@echo "$(GREEN)Opening API documentation...$(NC)"
	@open http://localhost:8080/swagger-ui.html || xdg-open http://localhost:8080/swagger-ui.html || start http://localhost:8080/swagger-ui.html

# Version info
version: ## Show version information
	@echo "$(BLUE)Member Auth System$(NC)"
	@echo "Docker version: $$(docker --version)"
	@echo "Docker Compose version: $$(docker-compose --version)"
	@echo "Java version in container:"
	@docker-compose exec app java -version 2>&1 || echo "$(YELLOW)Container not running$(NC)"
