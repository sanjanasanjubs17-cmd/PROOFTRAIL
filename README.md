# PROOFTRAIL — Evidence-first Brand Governance

A functional Round 2 prototype for reviewing marketing content against approved facts and brand rules. The app uses a Java Spring Boot API and a browser-based frontend.

## Features implemented
- Enter approved brand facts and guideline phrases.
- Submit draft content to a Java backend for rule-based analysis.
- View sentence-level statuses: Supported, Unsupported, Needs Human Review.
- Read the reason, matching evidence, and suggested action for each finding.
- Accept, edit, or dismiss findings; decisions are recorded in browser-local history.
- Export a JSON review report.
- Responsive layout and a health endpoint at `/api/health`.

## Important limitations
- This is a rule-based prototype, not a full AI fact-checker.
- “Supported” means a text overlap was found; it does not prove a claim is true.
- Review history is stored in the browser on the current device, not a shared database.
- Do not enter confidential or personal information in the demo.

## Requirements
- JDK 17 or newer
- Maven 3.6+ (or use your IDE's Maven support)
- Internet connection for the first build so Maven can download dependencies

Check versions:
```bash
java -version
javac -version
mvn -version
```

## Run locally
Open a terminal in the folder containing `pom.xml` and run:
```bash
mvn spring-boot:run
```
Then open http://localhost:8080

Health check: http://localhost:8080/api/health

## Build a runnable JAR
```bash
mvn clean package
java -jar target/prooftrail-1.0.0.jar
```

## Suggested deployment
Push this repository to GitHub and deploy it to a Java-capable hosting service that supports Maven builds and an environment `PORT` variable. Build command:
```bash
mvn clean package
```
Start command:
```bash
java -jar target/prooftrail-1.0.0.jar
```
After deployment, open the public URL in a private/incognito window and test the complete workflow. Do not submit a local `localhost` link as the project link.

## Demo test
1. Keep the sample approved facts and guidelines on the page.
2. Click **Check content**.
3. Confirm results include findings with reasons, evidence, and suggested actions.
4. Try Accept, Edit, and Dismiss.
5. Click **Export review** and confirm a JSON file downloads.
6. Reload the page and verify review history appears in the same browser.

## API example
`POST /api/check`
```json
{
  "approvedFacts": "Our bottle is made from 80% recycled aluminum.\nThe product warranty lasts 12 months.",
  "guidelines": "miracle cure\nguaranteed results",
  "content": "We guarantee results with our miracle cure."
}
```

This prototype is intended to be improved through testing. Do not claim unimplemented features as completed in the hackathon submission.
