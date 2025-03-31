const codeEditor = document.getElementById('codeEditor');
const lineNumbers = document.getElementById('lineNumbers');
const outputElement = document.getElementById('output');

// Line numbers setup
function updateLineNumbers() {
    const lines = codeEditor.value.split('\n');
    lineNumbers.innerHTML = lines.map((_, i) => i + 1).join('<br>');
}

codeEditor.addEventListener('input', updateLineNumbers);
codeEditor.addEventListener('scroll', () => {
    lineNumbers.scrollTop = codeEditor.scrollTop;
});
updateLineNumbers();

// Keyboard shortcut
codeEditor.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.key === 'Enter') {
        compileCode();
        e.preventDefault();
    }
});

async function compileCode() {
    const code = codeEditor.value;
    outputElement.classList.remove('error');
    outputElement.innerHTML = '🔄 Compiling and running...';

    try {
        const response = await fetch('/api/compile', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ code })
        });

        const result = await response.json();
        
        if (result.success) {
            outputElement.textContent = result.output || "✅ Program executed successfully (no output)";
        } else {
            outputElement.classList.add('error');
            outputElement.textContent = `❌ Error:\n${result.error}`;
        }
    } catch (error) {
        outputElement.classList.add('error');
        outputElement.textContent = `🚨 Request failed: ${error.message}`;
    }
}

function clearOutput() {
    outputElement.textContent = '';
    outputElement.classList.remove('error');
}