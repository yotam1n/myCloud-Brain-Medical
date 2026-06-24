# Running

JDK:
- Java 17
- Default path: `C:\Program Files\Java\jdk-17`

Backend with MySQL:
```powershell
docker compose -f docker-compose.mysql.yml up -d

$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/cloudbrain_medical?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="cloudbrain"
$env:DB_PASSWORD="cloudbrain_dev"
.\mvnw.cmd -pl backend spring-boot:run
```

Flyway runs the database migrations automatically when the backend starts.

Backend with default in-memory H2:
```powershell
.\mvnw.cmd -pl backend test
.\mvnw.cmd -pl backend spring-boot:run
```

Frontend:
```powershell
cd frontend
npm install
npm run dev
```

URLs:
- Backend: `http://localhost:8080`
- Health check: `http://localhost:8080/api/health`
- Frontend: `http://localhost:5173`
