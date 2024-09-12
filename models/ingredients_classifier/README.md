# Usage

This model is used to classify ingredients into grocery aisle. This is how to build/update it:

## Environment

tflite-model-maker is not working great with recent version of Python, so we use Python 3.9 here.

### Initialize environment

```
python3.9 -m venv .venv3.9
. .venv3.9/bin/activate
```

### Install dependencies

```
pip install tflite-model-maker==0.4.3
pip install scikit-learn
```

## Generate model

The resulting model is a tflite model embedded in the application.

This allow the app to work offline.

### Generate dataset

The first thing to do is to generate `train.csv` and `test.csv`.
Those two files are csv files with header: "Ingredient, Category"

To do the current model, you can call:

```bash
python generate_dataset.py
```

This will generate the two csv files.

### Train model

Then the next step is to train the model.

The TextClassifier can be trained by calling:

```bash
python classifier.py
# Then Move the model to the app
mv average_word_vec/model.tflite app/src/main/assets/ingredients_classifier.tflite
```