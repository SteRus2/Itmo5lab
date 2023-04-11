package us.obviously.itmo.prog.manager;

import us.obviously.itmo.prog.data.DataCollection;
import us.obviously.itmo.prog.data.DataStorage;
import us.obviously.itmo.prog.commands.*;
import us.obviously.itmo.prog.console.ConsoleColors;
import us.obviously.itmo.prog.console.Messages;
import us.obviously.itmo.prog.exceptions.*;
import us.obviously.itmo.prog.reader.DataReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


/**
 * @param <T>
 */
public class Manager<T> implements Management {
    private final Scanner scanner;
    private final DataCollection dataCollection;
    private final HashMap<String, AbstractCommand> commands;
    private final List<AbstractCommand> commandsList;
    private final List<String> loadedScripts = new ArrayList<>();
    private Scanner fileScanner;
    private Boolean active;

    public Manager(DataReader reader) throws IncorrectValueException, IncorrectValuesTypeException, CantParseDataException, CantFindFileException, FileNotReadableException {
        this.dataCollection = new DataStorage(reader);
        this.scanner = new Scanner(System.in);

        this.commands = new HashMap<>();
        this.commandsList = new ArrayList<>();
        new HelpCommand(this);
        new InfoCommand(this);
        new ShowCommand(this);
        new InsertCommand(this);
        new UpdateCommand(this);
        new RemoveKeyCommand(this);
        new ClearCommand(this);
        new SaveCommand(this);
        new ExecuteScriptCommand(this);
        new ExitCommand(this);
        new ReplaceIfGreaterCommand(this);
        new RemoveGreaterKeyCommand(this);
        new RemoveLowerKeyCommand(this);
        new GroupCountingByNameCommand(this);
        new FilterGreaterThanGroupAdminCommand(this);
        new PrintFieldAscendingSemesterEnumCommand(this);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Scanner getScanner() {
        if (this.fileScanner != null && this.fileScanner.hasNextLine()) return this.fileScanner;
        return this.scanner;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String nextChar() {
        if (this.fileScanner != null && this.fileScanner.hasNext()) {
            String line = this.fileScanner.next();
            Messages.print(ConsoleColors.GREEN_BOLD + line + "~=");
            return line;
        }
        return this.scanner.next();
    }

    /**
     * @inheritDoc
     */
    @Override
    public String nextLine() {
        if (this.fileScanner != null && this.fileScanner.hasNextLine()) {
            String line = this.fileScanner.nextLine();
            Messages.printStatement(ConsoleColors.GREEN_BOLD + line + "~=");
            return line;
        }
        this.loadedScripts.clear();
        return this.scanner.nextLine();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void run() {
        this.active = true;
        while (this.active) {
            waitCommand();
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void stop() {
        this.active = false;
    }


    /**
     * @inheritDoc
     */
    @Override
    public void executeScript(String filepath) throws FileNotFoundException, RecurrentExecuteScripts {
        if (this.loadedScripts.contains(filepath))
            throw new RecurrentExecuteScripts("Скрипт " + filepath + " начал вызываться рекуррентно.");
        var file = new File(filepath);
        this.fileScanner = new Scanner(file);
        this.loadedScripts.add(filepath);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void addCommand(AbstractCommand abstractCommand) {
        this.commands.put(abstractCommand.getKey(), abstractCommand);
        this.commandsList.add(abstractCommand);
    }


    /**
     * @inheritDoc
     */
    @Override
    public boolean isIdExists(Integer id) {
        return dataCollection.getData().get(id) != null;
    }


    /**
     * @inheritDoc
     */
    @Override
    public List<AbstractCommand> getCommands() {
        return this.commandsList;
    }


    /**
     * @inheritDoc
     */
    @Override
    public AbstractCommand getCommand(String key) {
        return this.commands.get(key);
    }

    private boolean waitCommand() {
        Messages.print("> ");
        String line = this.nextLine().trim();
        String[] words = line.split("\\s+");
        AbstractCommand command = null;
        String commandName = "";
        if (words.length > 0) {
            commandName = words[0];
            command = this.commands.get(commandName.toLowerCase());
        }
        if (command == null) {
            if (commandName.equals("")) {
                Messages.print("Введите " + ConsoleColors.GREEN + "help~=" + " для просмотра текущих команд.%n");
            } else {
                Messages.print("\"%s\" не является командой. Введите " + ConsoleColors.GREEN + "help~=" + " для просмотра текущих команд.%n", commandName);
            }
            return false;
        }
        try {
            var args = command.parseParameters(Arrays.copyOfRange(words, 1, words.length));

            command.execute(args);

        } catch (UnexpectedArgumentException | MissedArgumentException e) {
            Messages.printStatement(e.getMessage());
        }
        return false;
    }

    /**
     * @inheritDoc
     */
    @Override
    public DataCollection getDataCollection() {
        return dataCollection;
    }

}