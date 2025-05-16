
# ☕ Java Playground

Welcome to **Java Playground** — an online Java compiler built with Spring Boot that lets you write, compile, and execute Java code directly in your browser.

This project is ideal for learners, interview practice, and running small Java snippets on the fly — all without installing an IDE.

---

## ✨ Features

- 📝 **In-Browser Editor**: Write Java code using a simple web interface.
- ⚙️ **Live Compilation**: Sends your code to a Spring Boot backend for compilation.
- 📤 **Detailed Output**: View standard output or errors from both compilation and execution.
- 🚫 **Runtime Limits**: Execution is sandboxed with a timeout to prevent infinite loops.
- 🔢 **Line Numbering**: Lightweight frontend shows line numbers while you type.
- 🐳 **Dockerized**: Easily containerized for deployment on any platform.

---

## 🧱 Tech Stack

| Layer     | Tech Used                |
|-----------|--------------------------|
| Backend   | Spring Boot (Java 17), Maven |
| Compiler  | JavaCompiler API, ProcessBuilder |
| Frontend  | HTML, CSS, JavaScript    |
| DevOps    | Docker                   |

---

## 🚀 How It Works

1. User writes Java code in the browser.
2. On clicking **Run**, code is sent to `/api/compile` via a `POST` request.
3. Backend uses `JavaCompiler` to compile code in memory.
4. If compilation is successful, it runs the `.class` file using `ProcessBuilder`.
5. Output (or error) is returned to the UI and displayed.

---

## 🛠 Project Structure

\`\`\`
Java-PlayGround/
├── src/
│   ├── main/
│   │   ├── java/com/JavaPlayground/
│   │   │   ├── controller/CompilerController.java
│   │   │   ├── model/{CodeSubmission.java, CompilationResponse.java}
│   │   │   ├── service/CompilerService.java
│   │   │   └── JavaPlayGroundApplication.java
│   │   └── resources/
│   │       └── static/{index.html, style.css, script.js}
├── Dockerfile
├── pom.xml
└── README.md
\`\`\`

---

## 🧪 API Reference

### 🔧 Compile Java Code

- **URL:** `/api/compile`  
- **Method:** `POST`  
- **Request Body:**
\`\`\`json
{
  "code": "public class HelloWorld { public static void main(String[] args) { System.out.println(\"Hello, world!\"); }}"
}
\`\`\`
- **Response:**
\`\`\`json
{
  "success": true,
  "output": "Hello, world!\n",
  "error": ""
}
\`\`\`

---

## 🐳 Run with Docker

### 🏗 Build Docker Image

\`\`\`bash
docker build -t java-playground .
\`\`\`

### ▶️ Run Container

\`\`\`bash
docker run -p 8080:8080 java-playground
\`\`\`

Now open your browser at [http://localhost:8080](http://localhost:8080)

---

## 🧰 Getting Started Locally

### ✅ Prerequisites

- Java 17+
- Maven
- Docker (optional)

### 🧪 Run Locally

\`\`\`bash
git clone https://github.com/MSNiranjan29/Java-PlayGround.git
cd Java-PlayGround
./mvnw spring-boot:run
\`\`\`

Open your browser at [http://localhost:8080](http://localhost:8080)

---

## 🖼️ Screenshots

### Code Editor  
![Editor Screenshot](https://via.placeholder.com/800x400?text=Editor+Screenshot)

### Output Panel  
![Output Screenshot](https://via.placeholder.com/800x400?text=Output+Screenshot)

---

## 🤝 Contributing

Pull requests are welcome!  
To contribute:

1. Fork the repository
2. Create a new branch: `git checkout -b feature-name`
3. Make your changes and commit: `git commit -m "Add feature"`
4. Push to your fork: `git push origin feature-name`
5. Submit a pull request

---

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).

---

## 💡 What's Next (Phase 2)

- Add support for user authentication & saved snippets
- Syntax highlighting using Monaco Editor
- Multi-language support (Python, C++)
- Unit test support (JUnit style)
- Deployment to cloud (Railway, Render)

---

**Happy Coding! 🚀**
