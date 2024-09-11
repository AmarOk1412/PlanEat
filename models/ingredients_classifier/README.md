# Usage


## Environment

```
python3.9 -m venv .venv3.9
. .venv3.9/bin/activate
```

```
pip install tflite-model-maker==0.4.3
pip install scikit-learn
```

# Generate app/src/main/assets/ingredients_classifier.tflite

```bash
python generate_dataset.py
python classifier.py
mv average_word_vec/model.tflite app/src/main/assets/ingredients_classifier.tflite
```