import pandas as pd
import tensorflow as tf
from sklearn.preprocessing import MultiLabelBinarizer
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Embedding, LSTM
import json
import re

# Load your CSV data
df_train = pd.read_csv('train.csv')
df_test = pd.read_csv('test.csv')

# Convert Tags column from string representation to lists
def convert_to_list(tag_str):
    return tag_str.split(',')

df_train['Tags'] = df_train['Tags'].apply(convert_to_list)
df_test['Tags'] = df_test['Tags'].apply(convert_to_list)



# Clean and preprocess the text data
def clean_text(text):
    text = re.sub(r"[^a-zA-Z\s]", "", text)  # Remove special characters, digits, etc.
    text = re.sub(r"\s+", " ", text).strip()  # Remove extra spaces
    return text.lower()

# Function to extract and concatenate relevant fields from Recipe JSON
def process_recipe(recipe_json):
    # Load the JSON from string, if necessary
    recipe = json.loads(recipe_json) if isinstance(recipe_json, str) else recipe_json
    title = recipe.get('title', '')
    ingredients = ' '.join(recipe.get('ingredients', []))
    tags = ' '.join(recipe.get('tags', []))

    # Concatenate title, ingredients, and tags into one string
    full_text = f"{title} {ingredients} {tags}"
    return clean_text(full_text)

# Apply the recipe processing function
df_train['Recipe'] = df_train['Recipe'].apply(process_recipe)
df_test['Recipe'] = df_test['Recipe'].apply(process_recipe)


# Prepare the data
tokenizer = Tokenizer(oov_token="<OOV>", num_words=100000)
tokenizer.fit_on_texts(df_train['Recipe'])
X_train = tokenizer.texts_to_sequences(df_train['Recipe'])
X_test = tokenizer.texts_to_sequences(df_test['Recipe'])

print(df_train['Recipe'])
# Check how many words the tokenizer captured
print(f"Number of unique tokens: {len(tokenizer.word_index)}")

max_length = max(max(len(seq) for seq in X_train), max(len(seq) for seq in X_test))
X_train = pad_sequences(X_train, maxlen=max_length)
X_test = pad_sequences(X_test, maxlen=max_length)

# Convert tags to binary format
mlb = MultiLabelBinarizer()
y_train = mlb.fit_transform(df_train['Tags'])
y_test = mlb.transform(df_test['Tags'])

# Define the model
model = Sequential([
    Embedding(input_dim=len(tokenizer.word_index) + 1, output_dim=128, input_length=max_length),
    LSTM(64, return_sequences=True),
    LSTM(32),
    Dense(len(mlb.classes_), activation='sigmoid')
])

model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])

# Train the model
model.fit(X_train, y_train, epochs=50, batch_size=32, validation_split=0.1)

# Evaluate the model
loss, acc = model.evaluate(X_test, y_test)
print(f"Model evaluation results: Loss = {loss}, Accuracy = {acc}")

# Export the model to TensorFlow Lite format
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS, tf.lite.OpsSet.SELECT_TF_OPS]
converter._experimental_lower_tensor_list_ops = False
tflite_model = converter.convert()

# Save the model
with open('model_multi_label.tflite', 'wb') as f:
    f.write(tflite_model)

print("Model exported to TensorFlow Lite format.")

# Export tokenizer to JSON file
with open('tokenizer.json', 'w') as f:
    json.dump(tokenizer.word_index, f)

print("Tokenizer exported to 'tokenizer.json'.")

# Export labels to a text file
with open('labels.txt', 'w') as f:
    for label in mlb.classes_:
        f.write(f"{label}\n")

print("Labels exported to 'labels.txt'.")
