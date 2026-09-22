# Image 3 couches : (1) build Node → (2) runtime nginx seul → (3) config env.
# Couche 3 = VITE_API_URL injecté au build (Render static utilise le même).
ARG VITE_API_URL=http://localhost:8080
FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . ./
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
