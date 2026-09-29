console.log('InboxBuddy: AI email reply assistant loaded');

const API_URL = 'https://inboxbuddy-api.onrender.com/api/email/generate';

function getEmailContent(composeRoot) {
    const candidates = Array.from(document.querySelectorAll('.a3s.ail, .h7, .gmail_quote'))
        .filter((element) => !composeRoot?.contains(element))
        .map((element) => element.innerText.trim())
        .filter(Boolean);
    return candidates.at(-1) || '';
}

function findComposeRoot(toolbar) {
    return toolbar.closest('[role="dialog"], .aoI, .M9') || toolbar.parentElement;
}

function findComposeToolbar(root = document) {
    return root.querySelector('.btC, .aDh, .gU-Up');
}

function findComposeBox(root) {
    return root.querySelector(
        '[role="textbox"][g_editable="true"], [role="textbox"][contenteditable="true"], [contenteditable="true"]'
    );
}

function createAIButton() {
    const button = document.createElement('button');
    button.className = 'T-I J-J5-Ji aoO v7 T-I-atl L3 ai-reply-button';
    button.type = 'button';
    button.style.marginRight = '8px';
    button.textContent = 'AI Reply';
    button.setAttribute('data-tooltip', 'Generate AI Reply');
    return button;
}

function setComposeText(composeBox, text) {
    composeBox.focus();
    const inserted = document.execCommand('insertText', false, text);
    if (!inserted) composeBox.textContent = text;
    composeBox.dispatchEvent(new InputEvent('input', {
        bubbles: true, inputType: 'insertText', data: text
    }));
}

function injectButton(toolbar) {
    if (!toolbar || toolbar.querySelector('.ai-reply-button')) return;

    const composeRoot = findComposeRoot(toolbar);
    const button = createAIButton();
    button.addEventListener('click', async () => {
        const originalLabel = button.textContent;
        try {
            button.textContent = 'Generating...';
            button.disabled = true;
            const emailContent = getEmailContent(composeRoot);
            if (!emailContent) throw new Error('Open an email thread before generating a reply.');

            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ emailContent, tone: 'professional' }),
                signal: AbortSignal.timeout(30000)
            });
            const responseText = await response.text();
            if (!response.ok) throw new Error(responseText || `Backend request failed (${response.status})`);
            if (!responseText.trim()) throw new Error('Backend returned an empty reply.');

            const composeBox = findComposeBox(composeRoot);
            if (!composeBox) throw new Error('Gmail compose box was not found.');
            setComposeText(composeBox, responseText);
        } catch (error) {
            console.error('InboxBuddy failed to generate reply:', error);
            alert(`InboxBuddy: ${error.message}`);
        } finally {
            button.textContent = originalLabel;
            button.disabled = false;
        }
    });
    toolbar.insertBefore(button, toolbar.firstChild);
}

function scanForComposeWindows() {
    document.querySelectorAll('.btC, .aDh, .gU-Up').forEach(injectButton);
}

const observer = new MutationObserver(() => window.requestAnimationFrame(scanForComposeWindows));
observer.observe(document.body, { childList: true, subtree: true });
scanForComposeWindows();
