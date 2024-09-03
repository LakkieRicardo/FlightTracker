import re

def extract_regex_matches(file_path, regex_pattern):
    try:
        # Compile the regular expression pattern
        pattern = re.compile(regex_pattern)
        
        # Read the file content
        with open(file_path, 'r') as file:
            content = file.read()
        
        # Find all matches of the regex pattern in the content
        matches = pattern.findall(content)
        
        return matches
    except FileNotFoundError:
        print(f"Error: The file '{file_path}' was not found.")
        return []
    except Exception as e:
        print(f"An error occurred: {e}")
        return []

# Example usage:
if __name__ == "__main__":
    file_path = 'world.svg'  # Replace with your file path
    regex_pattern = r'd=".+">'
    
    matches = extract_regex_matches(file_path, regex_pattern)
    
    if matches:
        print("Matches found:")
        for match in matches:
            print(match)
    else:
        print("No matches found.")
