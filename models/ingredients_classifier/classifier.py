import pandas as pd

from tflite_model_maker import model_spec
from tflite_model_maker import text_classifier
from tflite_model_maker.text_classifier import DataLoader

import tensorflow as tf
assert tf.__version__.startswith('2')
tf.get_logger().setLevel('ERROR')

df_train = pd.read_csv('train.csv', engine="python")

df_test = pd.read_csv('test.csv', engine="python")

df_train.head()
spec = model_spec.get('average_word_vec')

train_data = DataLoader.from_csv(
      filename='train.csv',
      text_column='Ingredient',
      label_column='Category',
      model_spec=spec,
      is_training=True)

test_data = DataLoader.from_csv(
      filename='test.csv',
      text_column='Ingredient',
      label_column='Category',
      model_spec=spec,
      is_training=False)

model = text_classifier.create(train_data, model_spec=spec, epochs=200)

model.summary()
loss, acc = model.evaluate(test_data)
model.export(export_dir='average_word_vec')