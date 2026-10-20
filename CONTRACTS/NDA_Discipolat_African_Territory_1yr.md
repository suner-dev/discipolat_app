NON-DISCLOSURE AGREEMENT - DISCIPOLAT
This Non-Disclosure Agreement ("Agreement") is entered into as of [Date]
by and between:

[Your Organization Name or "Church Owner"] ("Disclosing Party"), and

ART4US, having its principal place of business in [African Country, e.g., Cameroon],
represented by its CEO Rainer Stefane Ngansop ("Receiving Party").

1. CONFIDENTIAL INFORMATION
   The Disclosing Party discloses Confidential Information including but not limited to:
   - Source code of Discipolat Church OS (all frontend, backend, mobile components)
   - Database schemas and migration scripts (Flyway V1-V204+)
   - Multi-tenant architecture and tenant isolation mechanisms
   - JWT RSA key implementations and encryption (AES-256-GCM)
   - Business rules for church management (members, contributions, groups)
   - Configuration files (.env, encryption keys, SMTP credentials)
   - Seed data and Flyway population scripts
   - API specifications and internal algorithms
   - Any technical documentation not publicly available

2. OBLIGATIONS OF THE RECEIVING PARTY
   - Hold all Discipolat Confidential Information in strict confidence
   - Not disclose to any third party without prior written consent from Disclosing Party
   - Use solely for: "maintenance/corrections sur instance de production ART4US"
   - Implement access controls (need-to-know basis only)
   - Not extract, copy, or store Confidential Information on personal devices except as strictly necessary

3. EXCEPTIONS
   Information not confidential if it: (a) publicly known through no fault of Receiving Party; 
   (b) known prior to disclosure; (c) independently developed without reference; (d) rightfully 
   received from third party with no confidentiality obligation.

4. TERM
   This Agreement remains in effect for **1 year** from the Effective Date.
   Confidentiality obligations survive for **3 years** after termination.
   
   [Note: User specified 1 year duration preference - this is the active term. The 3-year survival
   is standard for confidentiality to protect multi-tenant architecture.]

5. DISCIPOLAT-SPECIFIC PROVISIONS
   - License keys (JWT private/republic, AES key) must NEVER be shared or stored in developer's local environment
   - All work must be performed on Disclosing Party's authorized instances (Docker/Render)
   - Any bugs fixes must be submitted as patches, not by granting local code access
   - Breach of this Agreement regarding tenant data isolation constitutes material breach
   - Territory: Afrique (au moment de la rédaction de ce document)

6. GOVERNING LAW
   Laws of [Country, e.g., Cameroon / France], without regard to conflicts of law principles.

IN WITNESS WHEREOF, the parties have executed this Agreement as of the date first written above.

_______________________     _______________________
Disclosing Party            ART4US / Rainer Stefane Ngansop (CEO)