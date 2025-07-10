FROM node:latest as build

WORKDIR /app

COPY package*.json ./
RUN npm install --legacy-peer-deps
RUN npm install -g @angular/cli

COPY . .
RUN ng build


FROM nginx:latest

# Remove default nginx content
RUN rm -rf /usr/share/nginx/html/*

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the Angular app build output (from /browser subdirectory)
COPY --from=build /app/dist/financial-front/browser/ /usr/share/nginx/html/


EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]