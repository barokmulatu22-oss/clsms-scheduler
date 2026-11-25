// ui.js - helpers shared by every page. No business rules here, only display code.
// Each page needs <nav id="sidebar"> and <div id="messages"> in its HTML.

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

// Short for document.getElementById
function $(id) {
  return document.getElementById(id);
}

// Builds an element safely (text is never parsed as HTML).
// props: { class, onclick, ...any attribute }. children: strings, nodes, arrays or null.
function el(tag, props, ...children) {
  const node = document.createElement(tag);
  Object.keys(props || {}).forEach(key => {
    const value = props[key];
    if (value === null || value === undefined || value === false) return;
    if (key === 'class') node.className = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else node.setAttribute(key, value === true ? '' : value);
  });
  const add = child => {
    if (Array.isArray(child)) child.forEach(add);
    else if (child !== null && child !== undefined && child !== false) {
      node.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
  };
  children.forEach(add);
  return node;
}

// "MONDAY" -> "Monday"
function dayName(day) {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

// "09:00" stays "09:00"; null becomes ""
function formatTime(time) {
  return time ? String(time).slice(0, 5) : '';
}

function timeRange(start, end) {
  return formatTime(start) + ' - ' + formatTime(end);
}

// "CLASSROOM" -> "Classroom"
function typeLabel(type) {
  return type ? type.charAt(0) + type.slice(1).toLowerCase() : 'Any type';
}

// "09:30" -> 570 (minutes since midnight, used only for drawing)
function toMinutes(time) {
  const parts = String(time).split(':');
  return Number(parts[0]) * 60 + Number(parts[1]);
}

// ---------- messages ----------

function clearMessages() {
  $('messages').replaceChildren();
}

// Red box listing every problem. Accepts the { errors: [...] } object thrown by api.js.
function showErrors(err) {
  let errors = err && Array.isArray(err.errors) && err.errors.length > 0 ? err.errors : null;
  if (!errors) {
    console.error(err);
    errors = ['Something went wrong in the page. Please reload and try again.'];
  }
  const box = el('div', { class: 'alert alert-error', role: 'alert' },
    el('strong', {}, errors.length > 1 ? 'Please fix these problems:' : 'Problem:'),
    el('ul', {}, errors.map(message => el('li', {}, message))));
  $('messages').replaceChildren(box);
  box.scrollIntoView({ block: 'nearest' });
}

// Green confirmation box.
function showMessage(text) {
  const box = el('div', { class: 'alert alert-success', role: 'status' }, text);
  $('messages').replaceChildren(box);
  box.scrollIntoView({ block: 'nearest' });
}

// ---------- shared widgets ----------

const PAGES = [
  ['index.html', 'Schedule'],
  ['rooms.html', 'Rooms'],
  ['courses.html', 'Courses'],
  ['instructors.html', 'Instructors'],
  ['availability.html', 'Room Availability'],
  ['reports.html', 'Reports']
];

function renderSidebar(activePage) {
  // Skip link: first stop for keyboard users, jumps past the menu
  const main = document.querySelector('main');
  if (main && !$('skipLink')) {
    main.id = 'main';
    document.body.prepend(el('a', { id: 'skipLink', class: 'skip', href: '#main' }, 'Skip to content'));
  }
  $('sidebar').replaceChildren(
    el('div', { class: 'brand' }, 'CLSMS', el('small', {}, 'Classroom & Laboratory Scheduling')),
    el('div', { class: 'links' }, PAGES.map(page =>
      el('a', { href: page[0], 'aria-current': page[0] === activePage ? 'page' : null }, page[1])))
  );
}

// Fills a <select>. First option is the placeholder (value "").
function fillSelect(select, items, valueOf, labelOf, placeholder) {
  const keep = select.value;
  select.replaceChildren(el('option', { value: '' }, placeholder));
  items.forEach(item => select.append(el('option', { value: valueOf(item) }, labelOf(item))));
  select.value = keep; // keeps the choice if it is still in the list
}

// Fills a <tbody>. rows: array of arrays of cells (strings or nodes).
function fillRows(tbody, rows, columnCount, emptyText) {
  if (rows.length === 0) {
    tbody.replaceChildren(el('tr', {}, el('td', { colspan: columnCount, class: 'empty' }, emptyText)));
    return;
  }
  tbody.replaceChildren(...rows.map(cells => el('tr', {}, cells.map(cell => el('td', {}, cell)))));
}

// Runs an async button action: disables the button while it works, shows errors if it fails.
async function guarded(button, action) {
  button.disabled = true;
  try {
    await action();
  } catch (err) {
    showErrors(err);
  } finally {
    button.disabled = false;
  }
}
