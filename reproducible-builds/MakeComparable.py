import os
import shutil
import zipfile
import subprocess
import argparse

def main():

    # 0) Read script argument
    parser = argparse.ArgumentParser(description="Formatting apk files for comparison.")
    parser.add_argument(
        "-d", "--decompile",
        action="store_true",
        help="If set, decompile each .apk using apktool instead of unzipping."
    )
    args = parser.parse_args()

    # 1) Define folders 
    script_dir = os.path.dirname(os.path.abspath(__file__))
    apks_folder = os.path.join(script_dir, "apks")
    extracted_apks_folder = os.path.join(script_dir, "extracted_apks")
    built_apks_folder = os.path.join(extracted_apks_folder, "built-apks")
    splits_folder = os.path.join(built_apks_folder, "splits")
    playstore_apks_folder = os.path.join(extracted_apks_folder, "playstore-apks")

    # 2) Copy our apks output to extracted_apks. We do the reformatting in a copied directory so that we have still access to the original apk files in case it is needed.
    if os.path.exists(extracted_apks_folder):
        shutil.rmtree(extracted_apks_folder)
    shutil.copytree(apks_folder, extracted_apks_folder)

    # 2) Move .apk files from built-apks/splits to built-apks
    if os.path.exists(splits_folder):
        for filename in os.listdir(splits_folder):
            if filename.endswith(".apk"):
                src = os.path.join(splits_folder, filename)
                dst = os.path.join(built_apks_folder, filename)
                shutil.move(src, dst)

    # 3) Delete the now empty built-apks/splits folder
    if os.path.exists(splits_folder):
        shutil.rmtree(splits_folder)

    # 4) Delete built-apks/toc.pb
    toc_path = os.path.join(built_apks_folder, "toc.pb")
    if os.path.exists(toc_path):
        os.remove(toc_path)

    # 5) Rename built-apks/base-master.apk to base.apk
    base_master_path = os.path.join(built_apks_folder, "base-master.apk")
    new_base_path = os.path.join(built_apks_folder, "base.apk")
    if os.path.exists(base_master_path):
        os.rename(base_master_path, new_base_path)

    # 6) Rename the other two .apk files in built-apks (replacing 'base-' with 'split_config.')
    for filename in os.listdir(built_apks_folder):
        if filename.endswith(".apk") and "base-" in filename:
            old_file_path = os.path.join(built_apks_folder, filename)
            new_filename = filename.replace("base-", "split_config.", 1)
            new_file_path = os.path.join(built_apks_folder, new_filename)
            os.rename(old_file_path, new_file_path)

    # 7) Unzip all apks in built-apks/ and playstore-apks/, then delete original .apk files
    extract_apks_in_folder(built_apks_folder, decompile=args.decompile)
    extract_apks_in_folder(playstore_apks_folder, decompile=args.decompile)

def extract_apks_in_folder(folder_path, decompile=False):
    """
    Extracts all .apk files in the given folder and deletes the original .apk after extraction.
    Each .apk is extracted to a subfolder matching its filename without the .apk extension.
    """
    if not os.path.exists(folder_path):
        return
    for filename in os.listdir(folder_path):
        if filename.endswith(".apk"):
            apk_path = os.path.join(folder_path, filename)
            
            if decompile:
                decompile_apk(apk_path)
            else:
                unzip_apk(apk_path)
            
            # Remove the original APK file
            os.remove(apk_path)

def unzip_apk(apk_path):
    """
    Simply unzip the APK file.
    """
    extract_folder = os.path.splitext(apk_path)[0]
    os.makedirs(extract_folder, exist_ok=True)

    with zipfile.ZipFile(apk_path, 'r') as zip_ref:
        zip_ref.extractall(extract_folder)

def decompile_apk(apk_path):
    """
    Use apktool to decompile the provided APK file.
    """
    extract_folder = os.path.splitext(apk_path)[0]

    # Call apktool with 'd' (decode) command
    result = subprocess.run(["apktool", "d", apk_path, "-o", extract_folder], capture_output=True, text=True)

    # Check if it succeeded
    if result.returncode == 0:
        print("Successfully decoded:", apk_path)
        print("Output:", result.stdout)
    else:
        print("Error decoding:", apk_path)
        print("Error message:", result.stderr)

if __name__ == "__main__":
    main()