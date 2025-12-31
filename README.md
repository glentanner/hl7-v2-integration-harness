# HL7 v2 ADT Sender over MLLP (Java)

A hands-on, end-to-end project that demonstrates how HL7 v2 messages are
sent over raw TCP using the Minimal Lower Layer Protocol (MLLP),
validated with ACKs, and received by an interface engine (NextGen
Connect / Mirth) running in Docker.

This project simulates a real hospital registration system sending ADT
messages into an interface engine.

------------------------------------------------------------------------

## 🎯 What This Project Demonstrates

-   Building HL7 v2 **ADT\^A01** messages in Java\
-   Sending HL7 over **raw TCP sockets with MLLP framing**\
-   Receiving and parsing **HL7 ACKs** (AA / AE / AR)\
-   Correlating **MSH-10 ↔ MSA-2**\
-   Timeout and retry handling\
-   Running a Dockerized **NextGen Connect (Mirth)** listener\
-   Archiving inbound HL7 messages for audit/debug

This mirrors how many real-world hospital interfaces still work today.

------------------------------------------------------------------------

## 🧱 Architecture

    +------------------+        MLLP/TCP        +---------------------------+
    |  Java Sender     |  ------------------>  |  NextGen Connect (Mirth)  |
    |  (ADT Producer)  |  <------------------  |  TCP Listener + ACK       |
    +------------------+        HL7 ACK         +---------------------------+
                                                  |
                                                  v
                                          File Writer (archive)

------------------------------------------------------------------------

## 🛠️ Tech Stack

-   Java 25 (compiled for Java 21)
-   Maven
-   Docker & Docker Compose
-   NextGen Connect (Mirth) 4.5.2
-   Ubuntu 24.04
-   IntelliJ IDEA

------------------------------------------------------------------------

## 📋 Prerequisites

-   Java 21+ (Java 25 works)
-   Maven 3.9+
-   Docker & Docker Compose
-   Git

------------------------------------------------------------------------

## 🚀 Quick Start

### 1️⃣ Start Mirth in Docker

From the repo root:

``` bash
docker compose up -d
```

This starts NextGen Connect and exposes: - Web UI:
http://localhost:8080\
- Admin: https://localhost:8443\
- MLLP Listener: port **2575**

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
java -jar target/hl7-mllp-adt-sender-0.1.0.jar
```

------------------------------------------------------------------------

### 4️⃣ Verify Success

You should see logs like:

    Sending ADT^A01 attempt=1 controlId=...
    ACK msaCode=AA msaControlId=... correlated=true latencyMs=...
    SUCCESS

And a file in:

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
java -jar target/hl7-mllp-adt-sender-0.1.0.jar
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
- Rejected messages return **AR (Application Reject)**
- AR messages are **not retried** by the sender
- Failed payloads and ACKs are archived in `failed/`

This mirrors real-world interface engine behavior.

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

## 🏷️ Milestones

-   ✅ **Milestone 1:** ADT sender over MLLP with ACK handling\
- ✅ **Milestone 2:** Negative ACKs (AR/AE), retry rules, and failure archival
-   ⏳ Milestone 3: ADT workflow (A01 → A08 → A03)\
-   ⏳ Milestone 4: ORM / ORU messages\
-   ⏳ Milestone 5: JSON + streaming layer (Kafka)

------------------------------------------------------------------------

## 🧪 Troubleshooting

**No ACK / timeout** - Ensure channel is deployed - Check port 2575 is
listening: `bash   docker ps` - Check Mirth dashboard counters

**No files in `out/`** - Verify destination is `File Writer` - Confirm
directory is `/opt/connect/appdata/out` - Check inside container:
`bash   docker exec -it mirth ls /opt/connect/appdata/out`

**TLS warning** - Admin UI uses a self-signed cert --- safe to bypass
locally.

------------------------------------------------------------------------

## 🧹 Git Ignore

Archived HL7 files and build output should be ignored:

    target/
    out/
    .idea/
    *.iml

------------------------------------------------------------------------

## 📜 License

MIT
Copyright 2025 Glen Tanner

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

------------------------------------------------------------------------

## 👤 Author

Built by ** Glen Tanner** as a learning and portfolio project to gain hands-on experience with HL7 v2, MLLP, and healthcare integration patterns.
