# Build l'image
docker build -t mon-app-spring .

# Lancer le container
docker run -p 8080:8080 mon-app-spring