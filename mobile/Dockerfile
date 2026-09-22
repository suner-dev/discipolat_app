# Image 3 couches : (1) build Flutter → (2) runtime nginx pour web build → (3) config env.
# Note: Mobile apps are typically deployed via App Store / Play Store.
# This Dockerfile builds the Flutter Web variant for browser-based access / testing.
ARG FLUTTER_VERSION=3.27.0
FROM cirrusci/flutter:${FLUTTER_VERSION} AS build
WORKDIR /build
COPY pubspec.yaml pubspec.lock ./
RUN flutter pub get
COPY . .
RUN flutter build web --release

FROM nginx:alpine
COPY --from=build /build/build/web /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]