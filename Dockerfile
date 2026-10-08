FROM node:20-alpine

WORKDIR /usr/src/app

COPY package.json server.js index.html manifest.json sw.js ./
COPY pwa ./pwa
COPY app/src/main/res/drawable-nodpi ./images

ENV NODE_ENV=production
ENV PORT=8080

EXPOSE 8080

CMD ["node", "server.js"]
