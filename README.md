# Policy Assistant Spring Boot Application

This project provides a simple Spring Boot API that allows you to upload policy PDFs, persist them locally, and ask questions about their content using Amazon Bedrock's `gpt-4o-mini` model. Uploaded documents are parsed into searchable fragments so that answers are grounded in the available text. Every answer includes citations, and if the system cannot find a relevant answer it will return `I don't know.`

## Features

* **Upload PDFs** – `POST /upload` stores PDFs under `storage/pdfs/` and indexes their contents.
* **List stored files** – `GET /files` shows file metadata (name, size, upload time).
* **Delete stored files** – `DELETE /files/delete/{fileId}` removes the PDF and its extracted text.
* **Ask questions** – `POST /ask` queries the indexed PDFs, retrieves relevant fragments, and calls Amazon Bedrock for an answer with citations. If a chat is missing, it is created automatically with a sensible title.
* **Chat management** – `POST /chats/create`, `GET /chats`, and `DELETE /chats/delete/{chatId}` let you manage chat threads.
* **Open CORS policy** – All origins and common HTTP methods are allowed.

## Running locally

1. Ensure you have Java 17 and Maven installed.
2. Configure AWS credentials for Amazon Bedrock. You can provide the credentials via the standard AWS mechanisms (environment variables, `~/.aws/credentials`, or IAM role). The service expects the following environment variables to be present when running locally:
   ```bash
   export AWS_ACCESS_KEY_ID=YOUR_KEY
   export AWS_SECRET_ACCESS_KEY=YOUR_SECRET
   export AWS_SESSION_TOKEN=YOUR_SESSION_TOKEN # only if needed
   ```
3. Update `src/main/resources/application.properties` if you need to change the Bedrock region or storage location.
4. Build and run:
   ```bash
   mvn spring-boot:run
   ```

The API will start on `http://localhost:8080`.

## Running with Docker

You can build and run the service without installing Java or Maven by using the included Dockerfile.

1. Build the container image:
   ```bash
   docker build -t policy-assistant .
   ```
2. Create host directories to persist uploaded PDFs and the H2 database:
   ```bash
   mkdir -p storage/pdfs data
   ```
3. Run the container, providing your AWS credentials and mounting the persistence directories:
   ```bash
   docker run \
     --rm \
     -p 8080:8080 \
     -e AWS_ACCESS_KEY_ID=YOUR_KEY \
     -e AWS_SECRET_ACCESS_KEY=YOUR_SECRET \
     -e AWS_SESSION_TOKEN=YOUR_SESSION_TOKEN \
     -v "$(pwd)/storage:/app/storage" \
     -v "$(pwd)/data:/app/data" \
     policy-assistant
   ```

   Omit `AWS_SESSION_TOKEN` if it is not required for your credentials. The service will be available on `http://localhost:8080` as before.

To override application properties (for example, the Bedrock region), pass standard Spring Boot environment variables or JVM options, e.g.:

```bash
docker run \
  --rm \
  -p 8080:8080 \
  -e AWS_ACCESS_KEY_ID=YOUR_KEY \
  -e AWS_SECRET_ACCESS_KEY=YOUR_SECRET \
  -e SPRING_APPLICATION_JSON='{"bedrock":{"region":"us-west-2"}}' \
  -v "$(pwd)/storage:/app/storage" \
  -v "$(pwd)/data:/app/data" \
  policy-assistant
```

## Endpoints overview

| Method | Endpoint | Description |
| ------ | -------- | ----------- |
| `POST` | `/upload` | Upload a PDF file via `multipart/form-data` (`file` field). Returns `file uploaded: {filename}`. |
| `GET` | `/files` | List uploaded files with metadata. |
| `DELETE` | `/files/delete/{fileId}` | Delete the stored PDF and its extracted fragments. |
| `POST` | `/ask` | Ask a question. Body: `{ "question": "...", "fileIds": ["optional", "ids"], "chatId": "optional" }`. Returns the answer, citations (text snippet ≤100 chars, page, line), and chat metadata. |
| `POST` | `/chats/create` | Create a new chat (optional body `{ "title": "..." }`). |
| `GET` | `/chats` | List all chats. |
| `DELETE` | `/chats/delete/{chatId}` | Delete a chat and its messages. |

When `/ask` is called with a new chat or missing title, a contextual title is generated from the first question. If no relevant context is found, the answer is `I don't know.`

## Storage

* PDFs are stored under `storage/pdfs/` using a generated UUID for the filename.
* Document fragments and chat data are persisted in an H2 file database located at `./data/policy-assistant-db`.

## Notes

* Only non-empty text lines are stored as fragments to keep the index compact.
* Retrieval uses lightweight keyword scoring to select up to eight fragments that are passed to Bedrock.
* The application never serves answers from deleted PDFs because their fragments are removed when the file is deleted.

## Providing AWS credentials

After deploying or running the project, supply your AWS credentials using any supported AWS SDK method. The most common approaches are:

* Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, optional `AWS_SESSION_TOKEN`).
* The shared credentials file (`~/.aws/credentials`).
* IAM roles when running on AWS infrastructure.

No credentials should be committed to the repository.
