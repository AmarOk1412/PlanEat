from tflite_model_maker import model_spec
from tflite_model_maker import question_answer
from tflite_model_maker.question_answer import DataLoader

import tensorflow as tf
assert tf.__version__.startswith('2')
tf.get_logger().setLevel('ERROR')

#df_train = pd.read_csv('train.csv', error_bad_lines=False, engine="python")
#df_test = pd.read_csv('test.csv', error_bad_lines=False, engine="python")
#
#print(df_test.head())

spec = model_spec.get('mobilebert_qa_squad')

train_data_path = tf.keras.utils.get_file(
    fname='triviaqa-web-train-8000.json',
    origin='https://storage.googleapis.com/download.tensorflow.org/models/tflite/dataset/triviaqa-web-train-8000.json')
validation_data_path = tf.keras.utils.get_file(
    fname='triviaqa-verified-web-dev.json',
    origin='https://storage.googleapis.com/download.tensorflow.org/models/tflite/dataset/triviaqa-verified-web-dev.json')
train_data = DataLoader.from_squad(train_data_path, spec, is_training=True)
validation_data = DataLoader.from_squad(validation_data_path, spec, is_training=False)

#train_data = DataLoader.from_squad(filename='final.json',model_spec=spec,is_training=True,version_2_with_negative=True)
#test_data = DataLoader.from_squad(filename='final_val.json',model_spec=spec,is_training=False,version_2_with_negative=True)

model = question_answer.create(train_data, model_spec=spec, epochs=2, steps_per_epoch=50)

print(model.summary())

res = model.evaluate(test_data)

print('Loss:', res[0])
print('Accuracy:', res[1])

model.export(export_dir='mobilebert',
    tflite_filename='ingredients.tflite',
    label_filename='labels.txt',
    vocab_filename='vocab.txt',
    saved_model_filename='saved_model',
    tfjs_folder_name='tfjs',
    export_format=None)