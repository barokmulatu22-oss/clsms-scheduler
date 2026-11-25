// availability.js - Room Availability page: asks the API which rooms are free.

async function findRooms(event) {
  event.preventDefault();
  clearMessages();
  $('resultsWrap').hidden = true; // old results must not stay on screen if this search fails
  $('resultsNote').textContent = 'Searching...';
  await guarded($('searchBtn'), async () => {
    const rooms = await Api.availability({
      day: $('day').value,
      start: $('start').value,
      end: $('end').value,
      minCapacity: $('minCapacity').value,
      type: $('type').value
    });

    const when = dayName($('day').value) + ' ' + timeRange($('start').value, $('end').value);
    if (rooms.length === 0) {
      $('resultsWrap').hidden = true;
      $('resultsNote').hidden = false;
      $('resultsNote').textContent = 'No rooms are free on ' + when + ' with these filters.';
      return;
    }
    $('resultsNote').hidden = false;
    $('resultsNote').textContent = rooms.length + ' room(s) free on ' + when + '.';
    $('resultsWrap').hidden = false;
    fillRows($('rows'), rooms.map(room => [
      room.roomId,
      room.name,
      el('span', { class: 'badge ' + room.type.toLowerCase() }, typeLabel(room.type)),
      room.location,
      room.capacity
    ]), 5, '');
  });
  // If the search failed, the note must not stay on "Searching..."
  if ($('resultsNote').textContent === 'Searching...') {
    $('resultsNote').textContent = 'No results. Fix the problems above and try again.';
  }
  // On failure the form keeps what the user typed.
}

renderSidebar('availability.html');
$('day').replaceChildren(...DAYS.map(day => el('option', { value: day }, dayName(day))));
$('availabilityForm').addEventListener('submit', findRooms);
