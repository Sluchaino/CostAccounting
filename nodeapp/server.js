const express = require('express');
const app = express();
const port = 3001;
const nodeId = process.env.NODE_ID || 'unknown';

app.get('/', (req, res) => {
  const html = `
    <!DOCTYPE html>
    <html>
    <head>
        <title>Node Balancer</title>
        <style>
            body { font-family: Arial; text-align: center; padding-top: 50px; }
            h1 { font-size: 3em; }
            .node { color: #4CAF50; }
        </style>
    </head>
    <body>
        <h1>Welcome from <span class="node">Node ${nodeId}</span></h1>
        <p>Request served by container with ID: ${nodeId}</p>
    </body>
    </html>
  `;
  res.send(html);
});

// Healthcheck endpoint
app.get('/health', (req, res) => {
  res.status(200).send('OK');
});

app.listen(port, () => {
  console.log(`Node app listening at http://localhost:${port}, nodeId: ${nodeId}`);
});