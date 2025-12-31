# HL7 v2 Integration Harness (Java)

A self-directed, non-production HL7 v2 integration harness designed to simulate real-world healthcare message transport, acknowledgment semantics, failure handling, and workflow progression using MLLP over TCP.

This project focuses on **system behavior rather than UI configuration**, demonstrating how HL7 messages are constructed, transmitted, acknowledged (AA / AE / AR), retried, rejected, and archived in patterns commonly encountered in regulated healthcare systems.

A Dockerized instance of NextGen Connect (Mirth) is used to simulate an interface engine receiving messages from an upstream registration system.

---

## 🎯 Integration Behaviors Demonstrated

- HL7 v2 message construction (ADT events) in Java  
- MLLP framing over raw TCP sockets  
- ACK processing and correlation (AA / AE / AR)  
- Control ID correlation (MSH-10 ↔ MSA-2)  
- Timeout handling, retry rules, and backoff  
- Failure classification and archival for audit and replay  
- Interface engine simulation using Dockerized NextGen Connect (Mirth)

This mirrors how many real-world hospital interfaces still operate today.

---

## 🧱 Logical Architecture (Non-Production)

    +------------------+        MLLP/TCP        +---------------------------+
    |  Java Sender     |  ------------------>  |  NextGen Connect (Mirth)  |
    |  (ADT Producer)  |  <------------------  |  TCP Listener + ACK       |
    +------------------+        HL7 ACK         +---------------------------+
                                                  |
                                                  v
                                          File Writer (archive)

------------------------------------------------------------------------

## 🛠️ Tech Stack

- Java 25 (compiled for Java 21)
- Maven
- Docker & Docker Compose
- NextGen Connect (Mirth) 4.5.2
- Ubuntu 24.04
- IntelliJ IDEA

---

## 📋 Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- Git

---

## 🚀 Quick Start

### 1️⃣ Start Mirth in Docker

From the repo root:

```bash
docker compose up -d
```
------------------------------------------------------------------------

Services exposed:

Admin UI: https://localhost:8443

Web UI: http://localhost:8080

MLLP Listener: port 2575

Default login on first run:\
`admin / admin`

------------------------------------------------------------------------

### 2️⃣ Import the Mirth Channel

A preconfigured NextGen Connect (Mirth) channel is included.

In **Mirth Connect Administrator**:
1. Channels → Import
2. Import:
   `mirth/channels/LLP_Inbound_2575.xml`
3. Deploy the channel

The channel includes:
- TCP Listener (MLLP server) on port 2575
- Source Filter enforcing required PID segment
- Auto-generated ACKs (AA / AR)
- File Writer archiving inbound HL7


------------------------------------------------------------------------

### 3️⃣ Build and Run the Java Sender

``` bash
mvn clean package
java -jar target/hl7-v2-integration-harness-0.1.0.jar
```

------------------------------------------------------------------------

### 4️⃣ Verify Success

Expected logs:

    Sending ADT^A01 attempt=1 controlId=...
    ACK msaCode=AA msaControlId=... correlated=true latencyMs=...
    SUCCESS

Archived messages appear in:

``` bash
out/
  inbound_<timestamp>.hl7
```

Containing the HL7 message.

------------------------------------------------------------------------

## 📄 Example HL7 Message

    MSH|^~\&|JAVA_SENDER|HOSP|MIRTH|HOSP|20251219152533||ADT^A01|<uuid>|P|2.3
    PID|1||123456^^^HOSP^MR||DOE^JANE||19800101|F
    PV1|1|E|ER^01^01^HOSP|||||||||||||||V0001


------------------------------------------------------------------------

## ⚙️ Configuration

Environment variables:

  Variable                Default       Description
  ----------------------- ------------- ----------------
  `HL7_HOST`              `127.0.0.1`   MLLP host
  `HL7_PORT`              `2575`        MLLP port
  `HL7_READ_TIMEOUT_MS`   `5000`        ACK timeout
  `HL7_MAX_ATTEMPTS`      `3`           Retry attempts
  `HL7_BACKOFF_MS`        `500`         Retry delay

Example:

``` bash
HL7_HOST=127.0.0.1 HL7_PORT=2575 \
java -jar target/hl7-v2-integration-harness-0.1.0.jar
```

------------------------------------------------------------------------

## 🧠 Key Concepts

-   **MLLP Framing**\
    Messages are wrapped with:

    -   Start: `0x0B`
    -   End: `0x1C 0x0D`

-   **ACK Correlation**\
    `MSH-10` (control ID) must match `MSA-2` in the ACK.

-   **AA / AE / AR**\
    Accept, Error, Reject --- determines sender behavior.
    
## ❌ Negative ACK Handling

- Messages missing required segments (e.g. PID) are **rejected by Mirth**
    - Messages missing required segments (e.g., PID) are rejected
    - AR responses are not retried
    - Failed payloads and ACKs are archived for inspection

    (This mirrors real-world interface engine behavior)

------------------------------------------------------------------------

## 🗂️ Repo Structure

    .
    ├── src/main/java/com/medlydesign/hl7mllp
    │   ├── Main.java
    │   ├── MllpClient.java
    │   ├── Hl7AdtBuilder.java
    │   ├── Hl7Ack.java
    │   └── Hl7AckParser.java
    ├── docker-compose.yml
    ├── out/               # HL7 archives (gitignored)
    ├── pom.xml
    └── README.md

------------------------------------------------------------------------

## 🏁 Roadmap & Capability Progression

### ✅ HL7 v2 transport over MLLP with ACK handling

### ✅ Negative ACK processing (AE / AR), retry rules, and failure archival

### ⏳ ADT lifecycle workflow simulation (A01 → A08 → A03)

### ⏳ Clinical order and result messaging (ORM / ORU)

### ⏳ JSON transformation and streaming layer (Kafka)

------------------------------------------------------------------------

## 📜 License

MIT Copyright &copy; 2025 Glen Tanner

------------------------------------------------------------------------

## 👤 Author

Built by Glen Tanner as a self-directed integration project exploring HL7 v2 messaging, MLLP transport behavior, failure handling, and workflow progression in regulated healthcare systems.
