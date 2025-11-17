package com.example.smartmute;


import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartmute.R;
import com.example.smartmute.SmartMuteDatabaseHelper;
import com.example.smartmute.EmergencyContact;
import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsFragment extends Fragment {

    private RecyclerView contactsRecyclerView;
    private EmergencyContactsAdapter contactsAdapter;
    private List<EmergencyContact> contactsList;
    private SmartMuteDatabaseHelper databaseHelper;

    private Button btnAddContact, btnPickContact;
    private EditText etContactName, etPhoneNumber, etCallThreshold, etTimeWindow;
    private TextView tvPermissionStatus;

    private static final int CONTACTS_PERMISSION_CODE = 1002;

    public EmergencyContactsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_emergency_contacts, container, false);

        initializeViews(view);
        setupDatabase();
        setupRecyclerView();
        loadContacts();
        setupClickListeners();
        checkContactsPermission();

        return view;
    }

    private void initializeViews(View view) {
        contactsRecyclerView = view.findViewById(R.id.emergency_contacts_recycler_view);
        btnAddContact = view.findViewById(R.id.btn_add_emergency_contact);
        btnPickContact = view.findViewById(R.id.btn_pick_contact);
        etContactName = view.findViewById(R.id.et_contact_name);
        etPhoneNumber = view.findViewById(R.id.et_phone_number);
        etCallThreshold = view.findViewById(R.id.et_call_threshold);
        etTimeWindow = view.findViewById(R.id.et_time_window);
        tvPermissionStatus = view.findViewById(R.id.tv_permission_status);
    }

    private void setupDatabase() {
        databaseHelper = new SmartMuteDatabaseHelper(requireContext());
    }

    private void setupRecyclerView() {
        contactsList = new ArrayList<>();
        contactsAdapter = new EmergencyContactsAdapter(contactsList);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        contactsRecyclerView.setAdapter(contactsAdapter);
    }

    private void loadContacts() {
        contactsList.clear();
        contactsList.addAll(databaseHelper.getAllEmergencyContacts());
        contactsAdapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        btnAddContact.setOnClickListener(v -> addNewEmergencyContact());
        btnPickContact.setOnClickListener(v -> pickContactFromDevice());
    }

    private void checkContactsPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            tvPermissionStatus.setText("Contacts permission required to pick from device");
            tvPermissionStatus.setTextColor(getResources().getColor(R.color.metallic_silver));
        } else {
            tvPermissionStatus.setText("Contacts access granted");
            tvPermissionStatus.setTextColor(getResources().getColor(R.color.aqua_glow));
        }
    }

    private void addNewEmergencyContact() {
        String name = etContactName.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String thresholdStr = etCallThreshold.getText().toString().trim();
        String windowStr = etTimeWindow.getText().toString().trim();

        if (name.isEmpty()) {
            etContactName.setError("Contact name is required");
            return;
        }

        if (phone.isEmpty()) {
            etPhoneNumber.setError("Phone number is required");
            return;
        }

        if (thresholdStr.isEmpty()) {
            etCallThreshold.setError("Call threshold is required");
            return;
        }

        if (windowStr.isEmpty()) {
            etTimeWindow.setError("Time window is required");
            return;
        }

        int callThreshold = Integer.parseInt(thresholdStr);
        int timeWindow = Integer.parseInt(windowStr);

        // Validate reasonable values
        if (callThreshold < 1 || callThreshold > 10) {
            etCallThreshold.setError("Enter value between 1-10");
            return;
        }

        if (timeWindow < 1 || timeWindow > 60) {
            etTimeWindow.setError("Enter value between 1-60 minutes");
            return;
        }

        EmergencyContact contact = new EmergencyContact();
        contact.setName(name);
        contact.setPhoneNumber(phone);
        contact.setCallCountThreshold(callThreshold);
        contact.setWindowMinutes(timeWindow);
        contact.setRingOverride(true);

        long id = databaseHelper.addEmergencyContact(contact);
        if (id != -1) {
            Toast.makeText(requireContext(), "Emergency contact added", Toast.LENGTH_SHORT).show();
            clearForm();
            loadContacts();
        } else {
            Toast.makeText(requireContext(), "Failed to add contact", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickContactFromDevice() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_CODE);
            return;
        }

        openContactsPicker();
    }

    private void openContactsPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, CONTACTS_PERMISSION_CODE);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No contacts app available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CONTACTS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactsPicker();
                tvPermissionStatus.setText("Contacts access granted");
                tvPermissionStatus.setTextColor(getResources().getColor(R.color.aqua_glow));
            } else {
                Toast.makeText(requireContext(), "Contacts permission denied", Toast.LENGTH_SHORT).show();
                tvPermissionStatus.setText("Contacts permission denied");
                tvPermissionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACTS_PERMISSION_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            try {
                ContentResolver contentResolver = requireContext().getContentResolver();
                Cursor cursor = contentResolver.query(data.getData(), null, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    String contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

                    // Get phone number
                    Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null
                    );

                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));

                        // Populate form
                        etContactName.setText(name);
                        etPhoneNumber.setText(phoneNumber.replaceAll("\\s+", ""));

                        phoneCursor.close();
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error picking contact", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clearForm() {
        etContactName.setText("");
        etPhoneNumber.setText("");
        etCallThreshold.setText("3");
        etTimeWindow.setText("10");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    // Emergency Contacts Adapter
    private class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder> {
        private List<EmergencyContact> contacts;

        public EmergencyContactsAdapter(List<EmergencyContact> contacts) {
            this.contacts = contacts;
        }

        @NonNull
        @Override
        public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_contact, parent, false);
            return new ContactViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
            EmergencyContact contact = contacts.get(position);
            holder.bind(contact);
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        class ContactViewHolder extends RecyclerView.ViewHolder {
            private TextView tvName, tvPhone, tvSettings;
            private Button btnEdit, btnDelete;

            public ContactViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_contact_name);
                tvPhone = itemView.findViewById(R.id.tv_contact_phone);
                tvSettings = itemView.findViewById(R.id.tv_contact_settings);
                btnEdit = itemView.findViewById(R.id.btn_edit_contact);
                btnDelete = itemView.findViewById(R.id.btn_delete_contact);
            }

            public void bind(EmergencyContact contact) {
                tvName.setText(contact.getName());
                tvPhone.setText(contact.getPhoneNumber());
                tvSettings.setText(String.format("%d calls in %d minutes",
                        contact.getCallCountThreshold(), contact.getWindowMinutes()));

                btnEdit.setOnClickListener(v -> editContact(contact));
                btnDelete.setOnClickListener(v -> deleteContact(contact));
            }

            private void editContact(EmergencyContact contact) {
                // Populate form for editing
                etContactName.setText(contact.getName());
                etPhoneNumber.setText(contact.getPhoneNumber());
                etCallThreshold.setText(String.valueOf(contact.getCallCountThreshold()));
                etTimeWindow.setText(String.valueOf(contact.getWindowMinutes()));

                // Change button to update
                btnAddContact.setText("Update Contact");
                btnAddContact.setOnClickListener(v -> updateContact(contact));
            }

            private void updateContact(EmergencyContact contact) {
                contact.setName(etContactName.getText().toString().trim());
                contact.setPhoneNumber(etPhoneNumber.getText().toString().trim());
                contact.setCallCountThreshold(Integer.parseInt(etCallThreshold.getText().toString()));
                contact.setWindowMinutes(Integer.parseInt(etTimeWindow.getText().toString()));

                boolean updated = databaseHelper.updateEmergencyContact(contact);
                if (updated) {
                    Toast.makeText(requireContext(), "Contact updated", Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadContacts();
                    // Reset button
                    btnAddContact.setText("Add Contact");
                    btnAddContact.setOnClickListener(v -> addNewEmergencyContact());
                }
            }

            private void deleteContact(EmergencyContact contact) {
                boolean deleted = databaseHelper.deleteEmergencyContact(contact.getId());
                if (deleted) {
                    contacts.remove(contact);
                    notifyDataSetChanged();
                    Toast.makeText(requireContext(), "Contact deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
