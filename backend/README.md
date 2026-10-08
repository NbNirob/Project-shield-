# Project Shield Backend (Node.js + PostgreSQL + Prisma)

This directory contains the production Node.js & TypeScript REST API service for Project Shield.

## Features
- **PostgreSQL Database** with Prisma ORM (acidic transactions, unique idempotency keys, foreign keys).
- **Anti-Fraud & Replay Protection**: Server verifies unique `idempotencyKey` per reward event; double claims return 409 DUPLICATE and increment user risk score.
- **Server-Side Daily Earning Quota**: Calculates midnight UTC server time.
- **Atomic Withdrawals**: Deducts coins inside database transactions; rejection restores coins with audit log.
- **Role-Based Access Control**: USER vs ADMIN permissions.
- **Append-Only Audit Log**: Immutable tracking of critical security actions.
- **AdMob SSV Ready**: Prepared for Google Server-Side Verification ECDSA signatures.

## Running the Backend
1. `cd backend`
2. `npm install`
3. Configure `.env` with your `DATABASE_URL`
4. Run migrations: `npx prisma migrate dev --name init`
5. Start dev server: `npm run dev`
