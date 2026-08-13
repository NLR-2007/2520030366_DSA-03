file_name = "similarity_find.txt"
search_text = "Spring Boot"

f = 0

try:
    with open(file_name, "r", encoding="utf-8") as file:
        for line in file:
            f += line.count(search_text)

    print("Text:", search_text)
    print("Frequency of the word:", f)

except FileNotFoundError:
    print("File not found please check the spelling")

except Exception as e:
    print("File error:", e)