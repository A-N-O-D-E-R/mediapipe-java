#!/bin/bash

# Quick start script for running the video upload app with Docker

set -e

echo "=================================="
echo "MediaPipe Video Upload Application"
echo "=================================="
echo ""

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if docker is installed
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    print_warning "docker-compose not found. Will use 'docker compose' instead."
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

# Parse arguments
ACTION=${1:-up}

case $ACTION in
    up|start)
        print_info "Starting the application with Docker Compose..."
        $DOCKER_COMPOSE up -d --build
        echo ""
        print_success "Application started successfully!"
        echo ""
        echo "The application is now running at: http://localhost:8080"
        echo ""
        echo "Useful commands:"
        echo "  - View logs:        $DOCKER_COMPOSE logs -f"
        echo "  - Stop app:         $DOCKER_COMPOSE down"
        echo "  - Restart app:      $DOCKER_COMPOSE restart"
        echo "  - Health check:     curl http://localhost:8080/api/video/health"
        echo ""
        ;;

    down|stop)
        print_info "Stopping the application..."
        $DOCKER_COMPOSE down
        print_success "Application stopped."
        ;;

    logs)
        print_info "Showing application logs (Ctrl+C to exit)..."
        $DOCKER_COMPOSE logs -f
        ;;

    restart)
        print_info "Restarting the application..."
        $DOCKER_COMPOSE restart
        print_success "Application restarted."
        ;;

    rebuild)
        print_info "Rebuilding and restarting the application..."
        $DOCKER_COMPOSE down
        $DOCKER_COMPOSE up -d --build
        print_success "Application rebuilt and restarted."
        ;;

    status)
        print_info "Checking application status..."
        $DOCKER_COMPOSE ps
        echo ""
        if curl -f -s http://localhost:8080/api/video/health > /dev/null 2>&1; then
            print_success "Application is healthy and responding."
        else
            print_warning "Application is not responding or not healthy."
        fi
        ;;

    clean)
        print_info "Cleaning up Docker resources..."
        $DOCKER_COMPOSE down -v
        docker system prune -f
        print_success "Cleanup completed."
        ;;

    test)
        print_info "Testing the API endpoint..."
        if curl -f -s http://localhost:8080/api/video/health; then
            echo ""
            print_success "API is working correctly!"
        else
            echo ""
            print_warning "API is not responding. Make sure the application is running."
        fi
        ;;

    help|--help|-h)
        echo "Usage: ./docker-run.sh [COMMAND]"
        echo ""
        echo "Commands:"
        echo "  up, start    Start the application (default)"
        echo "  down, stop   Stop the application"
        echo "  logs         View application logs"
        echo "  restart      Restart the application"
        echo "  rebuild      Rebuild and restart the application"
        echo "  status       Check application status"
        echo "  test         Test the health endpoint"
        echo "  clean        Stop and remove all containers and volumes"
        echo "  help         Show this help message"
        echo ""
        echo "Examples:"
        echo "  ./docker-run.sh              # Start the application"
        echo "  ./docker-run.sh logs         # View logs"
        echo "  ./docker-run.sh stop         # Stop the application"
        ;;

    *)
        echo "Unknown command: $ACTION"
        echo "Run './docker-run.sh help' for usage information."
        exit 1
        ;;
esac
