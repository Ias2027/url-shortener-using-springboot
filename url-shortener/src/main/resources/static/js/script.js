// All calls go to the same Spring Boot app that serves this page,
// so relative paths work whether running on localhost or after deployment.
const API_BASE = '/api/urls';

const shortenForm = document.getElementById('shorten-form');
const urlInput = document.getElementById('url-input');
const expiryInput = document.getElementById('expiry-input');
const submitBtn = document.getElementById('submit-btn');
const errorBox = document.getElementById('error-box');
const resultBox = document.getElementById('result-box');
const resultLink = document.getElementById('result-link');
const resultOriginal = document.getElementById('result-original');
const copyBtn = document.getElementById('copy-btn');

shortenForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  hide(errorBox);
  hide(resultBox);
  submitBtn.disabled = true;
  submitBtn.textContent = 'Shortening…';

  try {
    const body = { url: urlInput.value.trim() };
    if (expiryInput.value) body.expiresInMinutes = Number(expiryInput.value);

    const res = await fetch(`${API_BASE}/shorten`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const data = await readJson(res);

    if (!res.ok) {
      throw new Error(data.message || 'Something went wrong');
    }

    resultLink.href = data.shortUrl;
    resultLink.textContent = data.shortUrl;
    resultOriginal.textContent = `→ redirects to: ${data.originalUrl}`;
    show(resultBox);
  } catch (err) {
    errorBox.textContent = err.message;
    show(errorBox);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Shorten →';
  }
});

copyBtn.addEventListener('click', async () => {
  try {
    await navigator.clipboard.writeText(resultLink.href);
    const original = copyBtn.textContent;
    copyBtn.textContent = 'Copied!';
    setTimeout(() => (copyBtn.textContent = original), 1200);
  } catch {
    errorBox.textContent = 'Clipboard access is unavailable. Select and copy the link instead.';
    show(errorBox);
  }
});

const statsForm = document.getElementById('stats-form');
const statsInput = document.getElementById('stats-input');
const statsResult = document.getElementById('stats-result');

statsForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  hide(statsResult);

  try {
    // Accept either a bare code ("4c92") or a full pasted short URL.
    const raw = statsInput.value.trim();
    let code = raw;
    if (raw.includes('/r/')) {
      const pastedUrl = new URL(raw, window.location.origin);
      const match = pastedUrl.pathname.match(/\/r\/([0-9a-zA-Z]{1,12})\/?$/);
      if (!match) throw new Error('Enter a short code or a valid /r/{code} link');
      code = match[1];
    }

    const res = await fetch(`${API_BASE}/${encodeURIComponent(code)}/stats`);
    const data = await readJson(res);

    if (!res.ok) throw new Error(data.message || 'Short code not found');

    statsResult.replaceChildren();
    statsResult.style.color = '';

    const count = document.createElement('div');
    const countLabel = document.createElement('b');
    countLabel.textContent = String(data.clickCount);
    count.append(countLabel, ` click${data.clickCount === 1 ? '' : 's'}`);

    const created = document.createElement('div');
    created.textContent = `created: ${new Date(data.createdAt).toLocaleString()}`;

    const expiry = document.createElement('div');
    expiry.textContent = data.expiresAt
      ? `expires: ${new Date(data.expiresAt).toLocaleString()}`
      : 'no expiry';

    statsResult.append(count, created, expiry);
    show(statsResult);
  } catch (err) {
    statsResult.textContent = err.message;
    statsResult.style.color = 'var(--danger)';
    show(statsResult);
  }
});

function show(el) { el.classList.remove('hidden'); }
function hide(el) { el.classList.add('hidden'); }

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return {};
  }
}
