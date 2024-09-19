import pandas as pd
from sklearn.model_selection import train_test_split

# Load the data with "@" as the separator
df = pd.read_csv('input.csv', sep='@')

# Split the data into train and test sets
train_df, test_df = train_test_split(df, test_size=0.1, random_state=42)

# Save the train and test sets to CSV files with "@" as the separator
train_df.to_csv('train.csv', index=False)
test_df.to_csv('test.csv', index=False)

print(f"Train and test data have been split. Train data: {len(train_df)} rows, Test data: {len(test_df)} rows.")
