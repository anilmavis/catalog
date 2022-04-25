package com.example.catalog;

import com.example.catalog.exception.item.ItemNotExistException;
import com.example.catalog.exception.tag.TagExistException;
import com.example.catalog.exception.type.TypeExistException;
import com.example.catalog.exception.type.TypeNotExistException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.*;

public class CatalogController {
    private final TypeContainer typeContainer = new TypeContainer();
    private final ItemContainer itemContainer = new ItemContainer();
    @FXML
    private Button helpButton;
    @FXML
    private Button exitButton;
    @FXML
    private TreeView<String> view;
    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button addTagButton;
    @FXML
    private TextField tagField;
    @FXML
    private TableView<List<String>> table;

    @FXML
    private void initialize() {
        TreeItem<String> root = new TreeItem<>();
        root.setExpanded(true);
        view.setRoot(root);
        view.getSelectionModel().select(root);
        List<String> bookFieldTypes = new ArrayList<>();
        bookFieldTypes.add("Title");
        bookFieldTypes.add("Authors");
        bookFieldTypes.add("Edition");
        bookFieldTypes.add("Purchase date");
        bookFieldTypes.add("Completion date");

        List<String> cdFieldTypes = new ArrayList<>();
        cdFieldTypes.add("Title");
        cdFieldTypes.add("Colour");

        try {
            typeContainer.add(new Type("book", bookFieldTypes), view);
            typeContainer.add(new Type("cd", cdFieldTypes), view);
        } catch (TypeExistException e) {
            e.printStackTrace();
        }
        helpButton.setOnAction((actionEvent -> onHelp()));
        exitButton.setOnAction(actionEvent -> onExit());
        view.setOnMouseClicked(mouseEvent -> onSelect());
        addButton.setOnAction((actionEvent -> onAdd()));
        editButton.setOnAction(actionEvent -> onEdit());
        removeButton.setOnAction(actionEvent -> onRemove());
        addTagButton.setOnAction(actionEvent -> onAddTag());
    }

    private void onSelect() {
        table.getColumns().clear();
        table.getItems().clear();

        TreeItem<String> treeItem = view.getSelectionModel().getSelectedItem();

        if (treeItem instanceof Type type) {
            for (String s : type.getFieldTypes()) {
                table.getColumns().add(new TableColumn<>(s));
            }
        } else if (treeItem instanceof Item item) {
            for (int i = 0; i < item.getType().getFieldTypes().size(); i++) {
                TableColumn<List<String>, String> column = new TableColumn<>(item.getType().getFieldTypes().get(i));
                int finalI = i;
                column.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().get(finalI)));
                table.getColumns().add(column);
            }
            TableColumn<List<String>, String> column = new TableColumn<>("tags");
            column.setCellValueFactory(e -> new SimpleStringProperty(item.getTags().toString()));
            table.getColumns().add(column);
            List<String> fieldValues = FXCollections.observableArrayList();
            fieldValues.addAll(item.getFieldValues());
            table.getItems().add(fieldValues);
        }
    }

    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Localisation.HELP_TITLE);
        alert.setHeaderText(Localisation.HELP_TEXT);
        alert.show();
    }

    private void onExit() {
        Platform.exit();
    }

    public void onAdd() {
        TreeItem<String> treeItem = view.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.ERROR);

        if (treeItem instanceof Item) {
            alert.setHeaderText(Localisation.SELECT + Localisation.ROOT + " or " + Localisation.TYPE);
            alert.show();
        } else if (treeItem instanceof Type type) {
            onAddItem(type, alert);
        } else {
            onAddType(alert);
        }
    }

    public void onAddItem(Type type, Alert alert) {
        Dialog<Item> dialog = new Dialog<>();
        dialog.setTitle(Localisation.ADD + " " + Localisation.ITEM);
        DialogPane pane = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK);
        dialog.setDialogPane(pane);
        dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(windowEvent -> dialog.close());
        GridPane content = new GridPane();
        pane.setContent(content);
        TextField name = new TextField(type.getName());
        content.add(new Label("Name"), 0, 0);
        content.add(name, 1, 0);
        List<TextField> textFields = new ArrayList<>();

        for (int i = 0; i < type.getFieldTypes().size(); i++) {
            textFields.add(new TextField());
        }

        for (int i = 0; i < textFields.size(); i++) {
            content.add(new Label(type.getFieldTypes().get(i)), 0, i + 2);
            content.add(textFields.get(i), 1, i + 2);
        }
        dialog.setResultConverter(buttonType -> {
            if (buttonType == null) {
                return null;
            }
            List<String> fieldValues = new ArrayList<>();

            for (TextField f : textFields) {
                fieldValues.add(f.getText());
            }
            return new Item(name.getText(), type, fieldValues);
        });
        Optional<Item> result = dialog.showAndWait();

        if (result.isPresent()) {
            Item item = result.get();

            if (item.getName().trim().isEmpty()) {
                alert.setHeaderText(Localisation.EMPTY_NAME);
                alert.show();
            } else {
                itemContainer.add(item);
            }
        }
    }

    private void onAddType(Alert alert) {
        Dialog<Type> dialog = new Dialog<>();
        dialog.setTitle(Localisation.ADD + " " + Localisation.TYPE);
        DialogPane pane = new DialogPane();
        pane.getButtonTypes().add(ButtonType.OK);
        dialog.setDialogPane(pane);
        GridPane content = new GridPane();
        pane.setContent(content);
        TextField name = new TextField();
        TextField fields = new TextField();
        content.add(new Label("Name"), 0, 0);
        content.add(name, 1, 0);
        content.add(new Label("Field types"), 0, 1);
        content.add(fields, 1, 1);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == null) {
                return null;
            }

            if (fields.getText().isBlank()) {
                alert.setHeaderText(Localisation.BLANK_FIELD);
                alert.show();
                return null;
            }
            List<String> fieldTypes = new ArrayList<>(List.of(fields.getText().split(",")));
            fieldTypes.replaceAll(String::trim);
            return new Type(name.getText(), fieldTypes);
        });
        dialog.setDialogPane(pane);
        Optional<Type> result = dialog.showAndWait();

        if (result.isPresent()) {
            try {
                Type type = result.get();

                if (type.getName().trim().isEmpty()) {
                    alert.setHeaderText(Localisation.EMPTY_NAME);
                    alert.show();
                } else {
                    typeContainer.add(type, view);
                }
            } catch (TypeExistException e) {
                alert.setHeaderText(e.getMessage());
                alert.show();
            }
        }
    }

    private void onEdit() {
        TreeItem<String> treeItem = view.getSelectionModel().getSelectedItem();
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane pane = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK);
        dialog.setDialogPane(pane);
        GridPane content = new GridPane();
        pane.setContent(content);
        content.add(new Label("Name"), 0, 0);
        TextField name = new TextField();
        content.add(name, 1, 0);
        List<TextField> textFields = new ArrayList<>();
        Alert alert = new Alert(Alert.AlertType.ERROR);

        if (treeItem instanceof Type type) {
            dialog.setTitle("Edit type");

            name.setText(type.getName());

            for (int i = 0; i < type.getFieldTypes().size(); i++) {
                textFields.add(new TextField(type.getFieldTypes().get(i)));
            }

            for (int i = 0; i < textFields.size(); i++) {
                content.add(new Label("Field type " + (i + 1)), 0, i + 2);
                content.add(textFields.get(i), 1, i + 2);
            }
            dialog.setResultConverter(buttonType -> buttonType);
            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                if (name.getText().trim().isEmpty()) {
                    alert.setHeaderText(Localisation.EMPTY_NAME);
                    alert.show();
                } else {
                    type.setName(name.getText());
                    List<String> fieldTypes = new ArrayList<>();

                    for (TextField f : textFields) {
                        if (f.getText().isBlank()) {
                            alert.setHeaderText(Localisation.BLANK_FIELD);
                            alert.show();
                            return;
                        }
                        fieldTypes.add(f.getText().trim());
                    }
                    type.setFieldTypes(fieldTypes);
                }
            }
        } else if (treeItem instanceof Item item) {
            dialog.setTitle("Edit item");
            name.setText(item.getName());

            for (int i = 0; i < item.getFieldValues().size(); i++) {
                textFields.add(new TextField(item.getFieldValues().get(i)));
            }

            for (int i = 0; i < textFields.size(); i++) {
                content.add(new Label("Field value " + (i + 1)), 0, i + 2);
                content.add(textFields.get(i), 1, i + 2);
            }
            content.add(new Label("Tags"), 0, textFields.size() + 2);
            TextField tagsField = new TextField(item.getTags().toString());
            content.add(tagsField, 1, textFields.size() + 2);
            dialog.setResultConverter(buttonType -> buttonType);
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.OK)) {
                if (name.getText().trim().isEmpty()) {
                    alert.setHeaderText(Localisation.EMPTY_NAME);
                    alert.show();
                } else {
                    item.setName(name.getText());
                    List<String> fieldValues = new ArrayList<>();

                    for (TextField f : textFields) {
                        fieldValues.add(f.getText());
                    }
                    item.setFieldValues(fieldValues);
                    Set<String> tags = new HashSet<>();
                    Collections.addAll(tags, tagsField.getText().trim().replaceAll("\\[", "").replaceAll("]", "").split(","));
                    item.setTags(tags);
                }
            }
        } else {
            alert.setHeaderText(Localisation.SELECTED_ROOT);
            alert.show();
        }
    }

    private void onRemove() {
        TreeItem<String> treeItem = view.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.ERROR);

        if (treeItem instanceof Type type) {
            try {
                typeContainer.remove(type, view, itemContainer);
            } catch (TypeNotExistException e) {
                alert.setHeaderText(e.getMessage());
                alert.show();
            }
        } else if (treeItem instanceof Item item) {
            try {
                itemContainer.remove(item);
            } catch (ItemNotExistException e) {
                alert.setHeaderText(e.getMessage());
                alert.show();
            }
        } else {
            alert.setHeaderText(Localisation.SELECTED_ROOT);
            alert.show();
        }
    }

    private void onAddTag() {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        if (tagField.getText().isBlank()) {
            alert.setHeaderText(Localisation.BLANK_TAG);
            alert.show();
        } else {
            TreeItem<String> treeItem = view.getSelectionModel().getSelectedItem();

            if (treeItem instanceof Item item) {
                try {
                    item.addTag(tagField.getText());
                } catch (TagExistException e) {
                    alert.setHeaderText(e.getMessage());
                    alert.show();
                }
            } else if (treeItem instanceof Type) {
                alert.setHeaderText(Localisation.SELECTED_TYPE);
                alert.show();
            } else {
                alert.setHeaderText(Localisation.SELECTED_ROOT);
                alert.show();
            }
            tagField.clear();
        }
    }
}
