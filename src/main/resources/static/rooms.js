// rooms.js - Rooms page: table with search, add, edit and delete (deactivate if booked).

let editingId = null; // room ID being edited, or null when adding

function roomFromForm() {
  const body = {
    roomId: $('roomId').value,
    name: $('name').value,
    type: $('type').value,
    location: $('location').value,
    capacity: Number($('capacity').value),
    description: $('description').value
  };
  if (editingId !== null) body.active = $('active').checked;
  return body;
}

function resetForm() {
  editingId = null;
  $('roomForm').reset();
  $('roomId').disabled = false;
  $('activeField').hidden = true;
  $('formTitle').textContent = 'Add room';
  $('saveBtn').textContent = 'Add room';
  $('stopEditBtn').hidden = true;
}

function startEdit(room) {
  clearMessages();
  editingId = room.roomId;
  $('roomId').value = room.roomId;
  $('roomId').disabled = true; // the ID cannot change
  $('name').value = room.name;
  $('type').value = room.type;
  $('location').value = room.location;
  $('capacity').value = room.capacity;
  $('description').value = room.description || '';
  $('active').checked = room.active;
  $('activeField').hidden = false;
  $('formTitle').textContent = 'Edit room ' + room.roomId;
  $('saveBtn').textContent = 'Save changes';
  $('stopEditBtn').hidden = false;
  $('name').focus();
}

async function removeRoom(room) {
  if (!confirm('Delete room ' + room.roomId + '? If it has bookings it will be deactivated instead.')) return;
  clearMessages();
  try {
    const result = await Api.deleteRoom(room.roomId);
    showMessage(result.message);
    if (editingId === room.roomId) resetForm();
    await loadRooms();
  } catch (err) {
    showErrors(err);
  }
}

async function loadRooms() {
  try {
    const rooms = await Api.rooms($('search').value);
    fillRows($('rows'), rooms.map(room => [
      room.roomId,
      room.name,
      el('span', { class: 'badge ' + room.type.toLowerCase() }, typeLabel(room.type)),
      room.location,
      room.capacity,
      room.active ? 'Active' : el('span', { class: 'badge inactive' }, 'Inactive'),
      [
        el('button', { type: 'button', class: 'btn secondary small', onclick: () => startEdit(room) }, 'Edit'),
        ' ',
        el('button', { type: 'button', class: 'btn danger small', onclick: () => removeRoom(room) }, 'Delete')
      ]
    ]), 7, 'No rooms found.');
  } catch (err) {
    fillRows($('rows'), [], 7, 'The rooms could not be loaded.');
    showErrors(err);
  }
}

async function saveRoom(event) {
  event.preventDefault();
  clearMessages();
  await guarded($('saveBtn'), async () => {
    const wasEditing = editingId !== null;
    const saved = wasEditing
      ? await Api.updateRoom(editingId, roomFromForm())
      : await Api.createRoom(roomFromForm());
    showMessage('Room ' + saved.roomId + (wasEditing ? ' updated.' : ' added.'));
    resetForm();
    await loadRooms();
  });
  // On failure the form keeps what the user typed.
}

renderSidebar('rooms.html');
$('roomForm').addEventListener('submit', saveRoom);
$('stopEditBtn').addEventListener('click', () => { clearMessages(); resetForm(); });
$('search').addEventListener('input', loadRooms);
loadRooms();
