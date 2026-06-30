# Stage 1 — build React app
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
RUN npm run build

# Stage 2 — serve with nginx on port 8080 (Cloud Run requirement)
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Remove default nginx config
RUN rm -f /etc/nginx/conf.d/default.conf.bak
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
