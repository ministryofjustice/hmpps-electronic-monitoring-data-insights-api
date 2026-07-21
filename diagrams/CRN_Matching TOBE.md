sequenceDiagram
    autonumber

    participant EMDI-UI as EMDI-UI API
    participant EMDI as EMDI API
    participant PAC as Probation Access Control API
    participant CPR as Core Person Record API
    participant PIEMDI as PI-EMDI API
    participant EMDIDataStore as EMDI Data Store

    EMDI-UI->>EMDI: Search for CRN

    EMDI->>PAC: Check LAO access for CRN
    PAC-->>EMDI: LAO access result

    alt LAO access permitted
        EMDI->>CPR: Lookup using CRN
        CPR-->>EMDI: Return NOMIS / PNC identifiers

        EMDI->>PIEMDI: Get Order ID from personal contact notes
        PIEMDI-->>EMDI: Return Order ID

        EMDI->>EMDIDataStore: Check EM data exists<br/>(including fuzzy matching and formatting)
        EMDIDataStore-->>EMDI: Match result

        EMDI-->>EMDI-UI: Return matched result
    else LAO access denied
        EMDI-->>EMDI-UI: Return 403 Forbidden
    end