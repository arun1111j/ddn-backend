import re
import os

bin_path = 'target/contracts/DocumentNotarization.bin'
java_path = 'src/main/java/com/notarize/contracts/DocumentNotarization.java'

# Read the compiled bytecode
with open(bin_path, 'r') as f:
    bytecode = f.read().strip()

# Read the Java file
with open(java_path, 'r') as f:
    java_content = f.read()

# Prepare the replacement string
# Ensure we prepend 0x if not present in the bin file (solc usually doesn't output 0x)
new_binary_line = f'public static final String BINARY = "0x{bytecode}";'

# Regex to find the existing BINARY constant definition
# It looks like: public static final String BINARY = "..." ;
pattern = r'public static final String BINARY = ".*";'

# Perform replacement
new_java_content = re.sub(pattern, new_binary_line, java_content)

# Write back to Java file
with open(java_path, 'w') as f:
    f.write(new_java_content)

print(f"Successfully updated BINARY in {java_path}")
