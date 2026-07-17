# predix-ctf-gateway

HTTP settlement service for PrediX matching-core (`CTF_GATEWAY_URL`, port **8083**).

Replaces `MockCtfExecutionGateway` when matching-core sets `predix.clients.ctf-gateway.mock=false`.

## API

| Method | Path | Matching-core |
|--------|------|---------------|
| `POST` | `/api/v1/ctf/submit-trade` | `submitTrade` |
| `POST` | `/api/v1/ctf/cancel` | `cancelOrderOnChain` |
| `GET` | `/api/v1/ctf/tx/{txHash}` | `queryTxStatus` |

Response envelope: `{ code, message, data: { txHash, status }, ... }` (submit/cancel) or `data: { txHash, status, confirmations }` (query).

## Run

```bash
cd predix-ctf-gateway
mvn test
mvn spring-boot:run
```

Default: `CTF_MOCK_CHAIN=true` (deterministic hashes, no RPC).

Real RPC (Amoy testnet):

```bash
export CTF_MOCK_CHAIN=false
export RPC_URL=...
export OPERATOR_PRIVATE_KEY=...
export CTF_OPS_ADDRESS=0x...
export CHAIN_ID=80002
mvn spring-boot:run
```

## Matching-core switch

```yaml
predix.clients.ctf-gateway.mock: false
CTF_GATEWAY_URL: http://localhost:8083
```

## MVP honesty

- Mock profile: idempotent submit by `tradeId`, suitable for integration without chain.
- Real profile: Web3j receipts + operator txs; full `PredixCtfOps.split` ABI binding is the next hardening step (payload still carries `marketId`/`outcomeId` from matching).
