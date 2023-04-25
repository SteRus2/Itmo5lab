package us.obviously.itmo.prog.common.actions;

import us.obviously.itmo.prog.common.action_models.VoidModel;
import us.obviously.itmo.prog.common.data.DataCollection;
import us.obviously.itmo.prog.common.model.StudyGroup;
import us.obviously.itmo.prog.common.serializers.GroupCountingSerializer;
import us.obviously.itmo.prog.common.serializers.VoidSerializer;
import us.obviously.itmo.prog.server.exceptions.FailedToDumpsEx;

import java.util.List;
import java.util.Map;

public class GroupCountingByNameAction extends Action<VoidModel, Map<String, List<StudyGroup>>> {
    public GroupCountingByNameAction() {
        super("counting-name", new VoidSerializer(), new GroupCountingSerializer());
    }

    @Override
    public Response execute(DataCollection dataCollection, VoidModel arguments) {
        var result = dataCollection.groupCountingByName();
        try {
            String body = this.getResponse().serialize(result);
            return new Response(body, ResponseStatus.OK);
        } catch (FailedToDumpsEx e) {
            return new Response(e.getMessage(), ResponseStatus.SERVER_ERROR);
        }
    }
}
