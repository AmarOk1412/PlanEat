import numpy as np
import tensorflow as tf
import pandas as pd
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from sklearn.preprocessing import MultiLabelBinarizer
import re
import json

# Example recipe data
recipes = [
    {"title": "pates au pesto", "tags": ["healthy", "pates"], "ingredients": ["pates", "pesto", "fromage"]},
    {"title": "dumplings au poireau", "tags": ["vegetarian", "dumplings"], "ingredients": ["poireau", "pate", "oignon"]},
    {"title": "muffins aux pepites de chocolat", "tags": ["dessert", "chocolat"], "ingredients": ["farine", "morceaux de chocolat", "sucre"]},
    {"title":"Chips de patates douces","tags":["amuse-gueule","chips de patates douces","chips","patate douce","sel","facile","bon marché","chips"],"ingredients":["800 g de patate douce","sel"]},
    {"title":"Pain aux 5 bananes et au son","tags":["dessert","banane","fruit","desserts","à congeler","santé","choix sain"],"ingredients":["375 ml  (1 1\/2 tasse) de farine tout usage non blanchie","180 ml  (3\/4 tasse) de farine d’avoine","30 ml  (2 c. à soupe) de son de blé","5 ml  (1 c. à thé) de poudre à pâte","5 ml  (1 c. à thé) de bicarbonate de soude","500 ml  (2 tasses) de bananes bien mûres écrasées à la fourchette (environ 5 bananes)","125 ml  (1\/2 tasse) de cassonade légèrement tassée","60 ml  (1\/4 tasse) de yogourt nature 0 %","45 ml  (3 c. à soupe) d’huile de canola","2   oeufs"]}
]


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

# Load training data
df_train = pd.read_csv('train.csv')

# Initialize and fit the tokenizer
tokenizer = Tokenizer()
tokenizer.fit_on_texts(df_train['Recipe'])

max_length = 262  # Replace with the actual max length from training

# Fit MultiLabelBinarizer with training data
mlb = MultiLabelBinarizer()
mlb.fit(df_train['Tags'].apply(lambda x: x.split(',')))  # Fit with the same tags as during training

def preprocess_recipe(recipe, tokenizer, max_length):
    """Preprocess a single recipe and convert to padded sequence."""
    sequence = tokenizer.texts_to_sequences([recipe])
    print(sequence)
    padded_sequence = pad_sequences(sequence, maxlen=max_length)
    return padded_sequence.astype(np.float32)

def predict_tags(recipe, model, tokenizer, mlb, max_length):
    """Predict tags for a given recipe using the provided model."""
    padded_sequence = preprocess_recipe(recipe, tokenizer, max_length)

    # Load the TFLite model
    interpreter = tf.lite.Interpreter(model_path='model_multi_label.tflite')
    interpreter.allocate_tensors()

    # Set input tensor
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    interpreter.set_tensor(input_details[0]['index'], padded_sequence)

    # Run inference
    interpreter.invoke()

    # Get and process the output
    output_data = interpreter.get_tensor(output_details[0]['index'])
    predictions = (output_data > 0.5).astype(int)

    # Convert predictions to tags
    predicted_tags = [tag for idx, tag in enumerate(mlb.classes_) if predictions[0][idx] == 1]
    return predicted_tags

# Predict tags for each recipe
for recipe in recipes:
    title = recipe['title']
    text = process_recipe(recipe)
    tags = predict_tags(text, None, tokenizer, mlb, max_length)
    print(f"Recipe: {title}\nPredicted Tags: {tags}\n")
