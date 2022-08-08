using ItemChanger.Internal.Menu;
using Modding;
using RandomizerMod;
using System.Diagnostics;
using System.IO;
using System.Linq;

namespace SpoilerViewerMod
{
    public class SpoilerViewerMod : Mod, ICustomMenuMod
    {
        public SpoilerViewerMod() : base("Spoiler Viewer") { }

        public override string GetVersion() => Version;

        public static string JarFile { get; }

        public static string Version { get; }

        static int ComputeHashCode(string path)
        {
            var sha1 = System.Security.Cryptography.SHA1.Create();
            var bytes = sha1.ComputeHash(File.OpenRead(path));

            // Hash the entire integer.
            int sum = 0;
            for (int i = 0; i < bytes.Length; i++)
            {
                int op = bytes[i];
                for (int j = 1; j < bytes.Length; j++)
                {
                    op = (op * 256) % 997;
                }
                sum = (sum + op) % 997;
            }

            return sum;
        }

        static SpoilerViewerMod()
        {
            var asm = typeof(SpoilerViewerMod).Assembly;
            var folder = Path.GetDirectoryName(asm.Location);
            JarFile = Path.Combine(folder, "HKSpoilerViewer.jar");

            var hash = (ComputeHashCode(JarFile) + ComputeHashCode(asm.Location)) % 997;
            System.Version v = asm.GetName().Version;
            Version = $"{v.Major}.{v.Minor}.{v.Build}+{hash.ToString().PadLeft(3, '0')}";
        }

        public bool ToggleButtonInsideMenu => false;

        public MenuScreen GetMenuScreen(MenuScreen modListMenu, ModToggleDelegates? _)
        {
            ModMenuScreenBuilder builder = new(Localization.Localize("Spoiler Viewer"), modListMenu);
            builder.AddButton(Localization.Localize("Open RawSpoiler.json"), null, () => LaunchHKSV(false));

            if (ModHooks.GetMod("ICDL Mod") is Mod)
            {
                builder.AddButton(Localization.Localize("Open ICDL ctx.json"), null, () => LaunchHKSV(true));
            }

            return builder.CreateMenuScreen();
        }

        static string MostRecentlyModifiedDir(string path)
        {
            var sortedDirs = new DirectoryInfo(path).GetDirectories()
                .OrderByDescending(f => f.LastWriteTime)
                .ToList();

            if (sortedDirs.Count == 0)
            {
                return null;
            }

            return sortedDirs[0].FullName;
        }

        string GetJsonPath(bool openICDL)
        {
            if (openICDL)
            {
                var mostRecent = MostRecentlyModifiedDir(ItemChangerDataLoader.ICDLMod.TempDirectory);
                if (mostRecent != null)
                {
                    mostRecent = MostRecentlyModifiedDir(mostRecent);
                }

                if (mostRecent == null)
                {
                    LogError("No recent ctx.json to open");
                    return "";
                }

                return Path.GetFullPath(Path.Combine(mostRecent, "ctx.json"));
            } else
            {
                return Path.GetFullPath(Path.Combine(RandomizerMod.Logging.LogManager.RecentDirectory, "RawSpoiler.json"));
            }
        }

        public void LaunchHKSV(bool openICDL)
        {
            var path = GetJsonPath(openICDL);
            if (path == "") return;

            Log($"Opening {path} with {JarFile}...");
            
            Process process = new();
            process.StartInfo.FileName = "java.exe";
            process.StartInfo.Arguments = $"-jar \"{JarFile}\" \"{GetJsonPath(openICDL)}\"";
            process.StartInfo.UseShellExecute = false;
            process.StartInfo.CreateNoWindow = true;
            process.Start();
        }
    }
}