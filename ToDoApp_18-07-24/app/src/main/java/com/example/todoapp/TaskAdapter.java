package com.example.todoapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Context context;

    public TaskAdapter(List<Task> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_layout, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // Bind data to UI elements
        Task task = taskList.get(position);
        holder.taskTextView.setText(task.getTaskText());
        holder.checkBox.setChecked(task.isChecked());

        // Set click listener for checkbox
        holder.checkBox.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task clickedTask = taskList.get(adapterPosition);
                clickedTask.setChecked(holder.checkBox.isChecked());

                // Update the task's isChecked field in Firebase
                DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference()
                        .child("tasks").child(clickedTask.getUserId()).child(clickedTask.getId());
                tasksRef.child("isChecked").setValue(clickedTask.isChecked())
                        .addOnSuccessListener(aVoid -> {
                            // Successfully updated in Firebase
                            Log.d("FirebaseUpdate", "Task isChecked updated successfully");
                        })
                        .addOnFailureListener(e -> {
                            // Failed to update in Firebase
                            Log.e("FirebaseUpdate", "Failed to update isChecked", e);
                            Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // Set click listener for edit button
        holder.editButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task clickedTask = taskList.get(adapterPosition);
                showEditDialog(clickedTask);
            }
        });

        // Set click listener for delete button
        holder.deleteButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task clickedTask = taskList.get(adapterPosition);
                // Remove the task from Firebase
                DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference()
                        .child("tasks").child(clickedTask.getUserId()).child(clickedTask.getId());
                tasksRef.removeValue()
                        .addOnSuccessListener(aVoid -> {
                            // Successfully removed from Firebase
                            Log.d("FirebaseDelete", "Task deleted successfully");
                            // Remove the task from the list and notify the adapter
                            taskList.remove(adapterPosition);
                            notifyItemRemoved(adapterPosition);
                            notifyItemRangeChanged(adapterPosition, taskList.size());
                            Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            // Failed to delete in Firebase
                            Log.e("FirebaseDelete", "Failed to delete task", e);
                            Toast.makeText(context, "Failed to delete task", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        // Return the size of the task list
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        public TextView taskTextView;
        public CheckBox checkBox;
        public ImageButton editButton;
        public ImageButton deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize UI elements
            taskTextView = itemView.findViewById(R.id.taskTextView);
            checkBox = itemView.findViewById(R.id.todoCheckBox);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public void addTask(Task task) {
        // Add a new task to the list
        taskList.add(task);
        // Notify the adapter that the data set has changed
        notifyItemInserted(taskList.size() - 1);
    }

    private void showEditDialog(Task task) {
        // Create an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Task");

        // Set up the input
        final EditText input = new EditText(context);
        input.setText(task.getTaskText());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newText = input.getText().toString();
            if (!newText.equals(task.getTaskText())) {
                task.setTaskText(newText);

                // Update the task's text field in Firebase
                DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference()
                        .child("tasks").child(task.getUserId()).child(task.getId());
                tasksRef.child("taskText").setValue(newText)
                        .addOnSuccessListener(aVoid -> {
                            // Successfully updated in Firebase
                            Log.d("FirebaseUpdate", "Task updated successfully");
                            Toast.makeText(context, "Task updated", Toast.LENGTH_SHORT).show();

                            // Notify the adapter that the data set has changed
                            notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> {
                            // Failed to update in Firebase
                            Log.e("FirebaseUpdate", "Failed to update task", e);
                            Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(context, "No changes made", Toast.LENGTH_SHORT).show();
            }

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
