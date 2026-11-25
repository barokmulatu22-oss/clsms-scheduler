// instructors.js - Instructors page: table with search, add, edit and delete.

let editingId = null; // instructor ID being edited, or null when adding

function personFromForm() {
  return {
    instructorId: $('instructorId').value,
    fullName: $('fullName').value,
    department: $('department').value,
    email: $('email').value,
    phone: $('phone').value
  };
}

function resetForm() {
  editingId = null;
  $('instructorForm').reset();
  $('instructorId').disabled = false;
  $('formTitle').textContent = 'Add instructor';
  $('saveBtn').textContent = 'Add instructor';
  $('stopEditBtn').hidden = true;
}

function startEdit(person) {
  clearMessages();
  editingId = person.instructorId;
  $('instructorId').value = person.instructorId;
  $('instructorId').disabled = true; // the ID cannot change
  $('fullName').value = person.fullName;
  $('department').value = person.department || '';
  $('email').value = person.email || '';
  $('phone').value = person.phone || '';
  $('formTitle').textContent = 'Edit instructor ' + person.instructorId;
  $('saveBtn').textContent = 'Save changes';
  $('stopEditBtn').hidden = false;
  $('fullName').focus();
}

async function removeInstructor(person) {
  if (!confirm('Delete instructor ' + person.fullName + ' (' + person.instructorId + ')? This cannot be undone.')) return;
  clearMessages();
  try {
    const result = await Api.deleteInstructor(person.instructorId);
    showMessage(result.message);
    if (editingId === person.instructorId) resetForm();
    await loadInstructors();
  } catch (err) {
    showErrors(err); // e.g. the instructor still has bookings
  }
}

async function loadInstructors() {
  try {
    const people = await Api.instructors($('search').value);
    fillRows($('rows'), people.map(person => [
      person.instructorId,
      person.fullName,
      person.department,
      person.email,
      person.phone,
      [
        el('button', { type: 'button', class: 'btn secondary small', onclick: () => startEdit(person) }, 'Edit'),
        ' ',
        el('button', { type: 'button', class: 'btn danger small', onclick: () => removeInstructor(person) }, 'Delete')
      ]
    ]), 6, 'No instructors found.');
  } catch (err) {
    fillRows($('rows'), [], 6, 'The instructors could not be loaded.');
    showErrors(err);
  }
}

async function saveInstructor(event) {
  event.preventDefault();
  clearMessages();
  await guarded($('saveBtn'), async () => {
    const wasEditing = editingId !== null;
    const saved = wasEditing
      ? await Api.updateInstructor(editingId, personFromForm())
      : await Api.createInstructor(personFromForm());
    showMessage('Instructor ' + saved.instructorId + (wasEditing ? ' updated.' : ' added.'));
    resetForm();
    await loadInstructors();
  });
  // On failure the form keeps what the user typed.
}

renderSidebar('instructors.html');
$('instructorForm').addEventListener('submit', saveInstructor);
$('stopEditBtn').addEventListener('click', () => { clearMessages(); resetForm(); });
$('search').addEventListener('input', loadInstructors);
loadInstructors();
