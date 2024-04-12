import numpy as np
from numpy.random import RandomState
import pandas as pd
import os
from tflite_model_maker import model_spec
from tflite_model_maker import text_classifier
from tflite_model_maker.config import ExportFormat
from tflite_model_maker.config import QuantizationConfig
from tflite_model_maker.text_classifier import AverageWordVecSpec
from tflite_model_maker.text_classifier import DataLoader
import tensorflow as tf
assert tf.__version__.startswith('2')
tf.get_logger().setLevel('ERROR')

df_train = pd.read_csv('train.csv', error_bad_lines=False, engine="python")
df_test = pd.read_csv('test.csv', error_bad_lines=False, engine="python")

print(df_test.head())

spec = model_spec.get('average_word_vec')

train_data = DataLoader.from_csv(
   filename='train.csv',
   text_column='sentence',
   label_column='label',
   model_spec=spec,
   is_training=True)
test_data = DataLoader.from_csv(
   filename='test.csv',
   text_column='sentence',
   label_column='label',
   model_spec=spec,
   is_training=False)

model = text_classifier.create(train_data, model_spec=spec, epochs=100)

print(model.summary())

loss, acc = model.evaluate(test_data)

print('Loss:', loss)
print('Accuracy:', acc)

model.export(export_dir='average_word_vec')