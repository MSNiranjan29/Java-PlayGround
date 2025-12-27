// ---------- CodeMirror Setup ----------
let editor = CodeMirror(document.getElementById("editor"), {
  value: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your name:");
        // The program will pause here until you type in the Input box and hit Enter
        String name = sc.nextLine();
        
        System.out.println("Enter your age:");
        int age = sc.nextInt();
        
        System.out.println("Hello " + name + ", you are " + age + "!");
    }
}`,
  mode: "text/x-java",
  theme: "dracula",
  lineNumbers: true,
  gutters: ["CodeMirror-linenumbers", "error-gutter"],
  extraKeys: {
    "Ctrl-Space": "autocomplete",
    // FIX: Add Shortcuts directly to the Editor
    "Ctrl-Enter": function (cm) {
      connectAndRun();
    },
    "Cmd-Enter": function (cm) {
      connectAndRun();
    }, // For Mac users
  },
});

// ---------- DOM Elements ----------
const container = document.getElementById("container");
const splitter = document.getElementById("splitter");
const editorPane = document.querySelector(".editor-pane");
const ioPane = document.querySelector(".io-pane");
const layoutSelect = document.getElementById("layoutSelect");
const runBtn = document.getElementById("runBtn");
const copyBtn = document.getElementById("copyBtn");
const outputEl = document.getElementById("outputContent");
const inputEl = document.getElementById("inputContent");

let editorSizePercent = 60;

// ---------- Layout Manager ----------
function setLayout(layout) {
  const isHorizontal = layout === "right" || layout === "left";
  container.className = `layout-${layout} ${
    isHorizontal ? "horizontal" : "vertical"
  }`;
  splitter.className = `splitter ${
    isHorizontal ? "splitter-vertical" : "splitter-horizontal"
  }`;
  if (isHorizontal) applyHorizontalSizing(editorSizePercent);
  else applyVerticalSizing(editorSizePercent);
}

function applyHorizontalSizing(pct) {
  pct = Math.max(10, Math.min(90, pct));
  editorPane.style.flex = `0 0 ${pct}%`;
  ioPane.style.flex = `1 1 ${100 - pct}%`;
  editor.refresh();
}

function applyVerticalSizing(pct) {
  pct = Math.max(10, Math.min(90, pct));
  editorPane.style.flex = `0 0 ${pct}%`;
  ioPane.style.flex = `1 1 ${100 - pct}%`;
  editor.refresh();
}

layoutSelect.addEventListener("change", (e) => setLayout(e.target.value));
setLayout("right");

// ---------- Splitter Logic ----------
let isDragging = false;
splitter.addEventListener("mousedown", () => {
  isDragging = true;
  document.body.style.userSelect = "none";
});
window.addEventListener("mouseup", () => {
  isDragging = false;
  document.body.style.userSelect = "";
});
window.addEventListener("mousemove", (e) => {
  if (!isDragging) return;
  const rect = container.getBoundingClientRect();
  const isHorizontal = container.classList.contains("horizontal");
  let pct = isHorizontal
    ? ((e.clientX - rect.left) / rect.width) * 100
    : ((e.clientY - rect.top) / rect.height) * 100;
  if (
    container.classList.contains("layout-left") ||
    container.classList.contains("layout-top")
  )
    pct = 100 - pct;
  editorSizePercent = pct;
  isHorizontal ? applyHorizontalSizing(pct) : applyVerticalSizing(pct);
});

// ---------- ERROR HIGHLIGHTING ----------
let errorLines = [];

function clearErrors() {
  errorLines.forEach((line) => {
    editor.removeLineClass(line, "background", "cm-error-line");
    editor.setGutterMarker(line, "error-gutter", null);
  });
  errorLines = [];
}

function highlightErrorLine(lineNumber) {
  const lineIndex = lineNumber - 1;
  if (editor.getLine(lineIndex) === undefined) return;
  editor.addLineClass(lineIndex, "background", "cm-error-line");
  errorLines.push(lineIndex);
  const dot = document.createElement("div");
  dot.className = "error-gutter-marker";
  editor.setGutterMarker(lineIndex, "error-gutter", dot);
}

// ---------- WEBSOCKET LOGIC ----------
let socket = null;

function connectAndRun() {
  clearErrors();
  outputEl.textContent = "Connecting to server...\n";

  // Visual feedback
  runBtn.innerHTML = '<span class="btn-icon">⏳</span> Running...';
  runBtn.disabled = true;

  if (socket) {
    socket.close();
  }

  socket = new WebSocket("ws://localhost:8080/terminal");

  socket.onopen = () => {
    outputEl.textContent = "Connected. Compiling...\n";
    socket.send("RUN:" + editor.getValue());
    inputEl.focus();
  };

  socket.onmessage = (event) => {
    const msg = event.data;

    if (msg.startsWith("OUTPUT:")) {
      outputEl.textContent += msg.substring(7);
      outputEl.scrollTop = outputEl.scrollHeight;
    } else if (msg.startsWith("ERROR:")) {
      const errorText = msg.substring(6);
      outputEl.textContent += "\n[Error]\n" + errorText + "\n";
      const lines = errorText.split("\n");
      lines.forEach((line) => {
        const match = line.match(/Line\s+(\d+):/) || line.match(/:(\d+):/);
        if (match) highlightErrorLine(parseInt(match[1]));
      });
    } else if (msg.startsWith("EXIT:")) {
      outputEl.textContent += "\n=== " + msg.substring(5) + " ===\n";
      socket.close();
    }
  };

  socket.onclose = () => {
    runBtn.innerHTML = '<span class="btn-icon">▶</span> Run';
    runBtn.disabled = false;
  };

  socket.onerror = (error) => {
    outputEl.textContent += "\n[Connection Error. Is the server running?]\n";
    runBtn.innerHTML = '<span class="btn-icon">▶</span> Run';
    runBtn.disabled = false;
  };
}

// ---------- INPUT HANDLING ----------
inputEl.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    e.preventDefault();
    if (socket && socket.readyState === WebSocket.OPEN) {
      const text = inputEl.value;
      socket.send("INPUT:" + text);
      outputEl.textContent += text + "\n";
      outputEl.scrollTop = outputEl.scrollHeight;
      inputEl.value = "";
    } else {
      alert("Program is not running! Click 'Run' first.");
    }
  }
});

// ---------- BUTTON ACTIONS ----------
function copyCode() {
  navigator.clipboard.writeText(editor.getValue());
  // Just a visual feedback without changing the icon structure too much
  const oldText = copyBtn.innerHTML;
  copyBtn.innerHTML = '<span class="btn-icon">✓</span> Copied';
  setTimeout(() => (copyBtn.innerHTML = oldText), 1000);
}

runBtn.addEventListener("click", connectAndRun);
copyBtn.addEventListener("click", copyCode);
window.addEventListener("resize", () => editor.refresh());
