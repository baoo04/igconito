# Ticket Booking Service

Orchestrates checkout: hold → validate customer → payment → confirm seats → persist booking → notify.

Set downstream base URLs via `clients.*.base-url` (see `application.yml`). Optional header **`X-Idempotency-Key`** (replay-safe for 5 minutes).

Host port in compose default **5005** → container **5000**.
